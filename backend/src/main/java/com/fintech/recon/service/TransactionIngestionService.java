package com.fintech.recon.service;

import com.fintech.recon.config.RabbitMqConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import com.fintech.recon.service.ingestion.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionIngestionService {

    private final TransactionRepository transactionRepository;
    private final CsvParser csvParser;
    private final RabbitTemplate rabbitTemplate;

    public void ingestCsv(MultipartFile file, String bankName) {
        try {
            List<Transaction> transactions = csvParser.parse(file.getInputStream(), bankName);
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);
            log.info("Ingested {} transactions from {} CSV", savedTransactions.size(), bankName);
            
            // Publish event to RabbitMQ for async reconciliation
            savedTransactions.forEach(txn -> 
                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_TRANSACTION, 
                    RabbitMqConfig.ROUTING_KEY_INGESTED, 
                    txn
                )
            );
            
        } catch (IOException e) {
            log.error("Failed to parse CSV file", e);
            throw new RuntimeException("Failed to parse CSV file", e);
        }
    }
}
