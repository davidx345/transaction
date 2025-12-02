package com.fintech.recon.service;

import com.fintech.recon.config.RabbitMqConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.dto.CsvParseResult;
import com.fintech.recon.dto.IngestionResult;
import com.fintech.recon.infrastructure.TransactionRepository;
import com.fintech.recon.security.SecurityUtils;
import com.fintech.recon.service.ingestion.CsvParserImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionIngestionService {

    private final TransactionRepository transactionRepository;
    private final CsvParserImpl csvParser;
    private final RabbitTemplate rabbitTemplate;
    private final SecurityUtils securityUtils;

    /**
     * Ingest CSV file with specified bank name
     */
    public IngestionResult ingestCsv(MultipartFile file, String bankName) {
        try {
            log.info("Ingesting CSV file: {}, bank: {}", file.getOriginalFilename(), bankName);
            
            UUID userId = securityUtils.getCurrentUserId();
            List<Transaction> transactions = csvParser.parse(file.getInputStream(), bankName);
            
            // Set userId for each transaction
            transactions.forEach(txn -> txn.setUserId(userId));
            
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);
            log.info("Ingested {} transactions from {} CSV for user {}", savedTransactions.size(), bankName, userId);
            
            // Publish event to RabbitMQ for async reconciliation
            savedTransactions.forEach(txn -> 
                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_TRANSACTION, 
                    RabbitMqConfig.ROUTING_KEY_INGESTED, 
                    txn
                )
            );
            
            return IngestionResult.builder()
                .success(true)
                .fileName(file.getOriginalFilename())
                .bankName(bankName)
                .totalRecords(savedTransactions.size())
                .successfulRecords(savedTransactions.size())
                .failedRecords(0)
                .message("CSV ingested successfully")
                .build();
            
        } catch (IOException e) {
            log.error("Failed to parse CSV file", e);
            return IngestionResult.builder()
                .success(false)
                .fileName(file.getOriginalFilename())
                .bankName(bankName)
                .message("Failed to parse CSV file: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Ingest CSV file with auto-detection of bank format
     */
    public IngestionResult ingestCsvWithAutoDetection(MultipartFile file) {
        try {
            log.info("Ingesting CSV file with auto-detection: {}", file.getOriginalFilename());
            
            byte[] fileBytes = file.getBytes();
            CsvParseResult parseResult = csvParser.parseWithDetails(
                new ByteArrayInputStream(fileBytes), 
                "auto"
            );
            
            String detectedBank = parseResult.getDetectedFormat() != null 
                ? parseResult.getDetectedFormat().getDisplayName() 
                : "Unknown";
            
            log.info("Detected bank format: {}", detectedBank);
            
            UUID userId = securityUtils.getCurrentUserId();
            
            // Convert parsed transactions to domain entities and save
            List<Transaction> transactions = parseResult.getTransactions().stream()
                .map(this::convertToTransaction)
                .toList();
            
            // Set userId for each transaction
            transactions.forEach(txn -> txn.setUserId(userId));
            
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);
            
            // Publish events for reconciliation
            savedTransactions.forEach(txn -> 
                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_TRANSACTION, 
                    RabbitMqConfig.ROUTING_KEY_INGESTED, 
                    txn
                )
            );
            
            log.info("Auto-ingested {} transactions ({} errors)", 
                savedTransactions.size(), parseResult.getFailedRows());
            
            return IngestionResult.builder()
                .success(true)
                .fileName(file.getOriginalFilename())
                .bankName(detectedBank)
                .autoDetected(true)
                .totalRecords(parseResult.getTotalRows())
                .successfulRecords(savedTransactions.size())
                .failedRecords(parseResult.getFailedRows())
                .skippedRecords(parseResult.getSkippedRows())
                .warnings(parseResult.getWarnings())
                .parseErrors(parseResult.getErrors().stream()
                    .map(e -> String.format("Row %d: %s", e.getRow(), e.getMessage()))
                    .toList())
                .message(String.format("Ingested %d transactions from %s format", 
                    savedTransactions.size(), detectedBank))
                .build();
            
        } catch (IOException e) {
            log.error("Failed to process CSV file", e);
            return IngestionResult.builder()
                .success(false)
                .fileName(file.getOriginalFilename())
                .message("Failed to process CSV file: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Get list of supported banks
     */
    public List<String> getSupportedBanks() {
        return csvParser.getSupportedBanks();
    }
    
    /**
     * Convert parsed transaction to domain entity
     */
    private Transaction convertToTransaction(CsvParseResult.ParsedTransaction parsed) {
        return Transaction.builder()
            .source("bank")
            .externalReference(parsed.getExternalReference())
            .normalizedReference(parsed.getNormalizedReference())
            .amount(parsed.getAmount())
            .currency(parsed.getCurrency() != null ? parsed.getCurrency() : "NGN")
            .timestamp(parsed.getTimestamp())
            .status(parsed.getStatus())
            .customerIdentifier(parsed.getCustomerIdentifier())
            .rawData(parsed.getRawData())
            .build();
    }
}
