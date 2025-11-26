package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import com.fintech.recon.dto.CsvParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced CSV parser with auto-detection and multi-bank support.
 * Supports Nigerian banks: GTBank, Access Bank, Zenith Bank, First Bank, UBA
 * Also supports payment gateways: Paystack, Flutterwave
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvParserImpl implements CsvParser {

    private final List<BankCsvStrategy> strategyList;
    private final BankFormatDetector bankFormatDetector;
    
    private Map<String, BankCsvStrategy> strategies;
    
    /**
     * Get strategy map (lazy initialization)
     */
    private Map<String, BankCsvStrategy> getStrategies() {
        if (strategies == null) {
            strategies = strategyList.stream()
                .collect(Collectors.toMap(BankCsvStrategy::getBankName, Function.identity()));
        }
        return strategies;
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String bankName) {
        // If bank name provided, use specific strategy
        if (bankName != null && !bankName.isEmpty() && !"auto".equalsIgnoreCase(bankName)) {
            return Optional.ofNullable(getStrategies().get(bankName))
                .map(strategy -> strategy.parse(inputStream))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported bank: " + bankName));
        }
        
        // Auto-detect mode
        return parseWithAutoDetection(inputStream);
    }
    
    /**
     * Parse CSV with auto-detection of bank format
     */
    public List<Transaction> parseWithAutoDetection(InputStream inputStream) {
        try {
            // Read entire file into memory for detection
            byte[] fileBytes = inputStream.readAllBytes();
            
            // Detect bank format
            BankFormat detectedFormat = bankFormatDetector.detectFromBytes(fileBytes);
            log.info("Auto-detected bank format: {}", detectedFormat.getDisplayName());
            
            // Get appropriate strategy
            BankCsvStrategy strategy = getStrategyForFormat(detectedFormat);
            
            // Parse with detected strategy
            return strategy.parse(new ByteArrayInputStream(fileBytes));
            
        } catch (IOException e) {
            log.error("Error reading CSV file for auto-detection", e);
            throw new RuntimeException("Error processing CSV file", e);
        }
    }
    
    /**
     * Parse CSV with full details (for APIs that need metadata)
     */
    public CsvParseResult parseWithDetails(InputStream inputStream, String bankName) {
        try {
            byte[] fileBytes = inputStream.readAllBytes();
            
            // Determine format
            BankFormat format;
            if (bankName != null && !bankName.isEmpty() && !"auto".equalsIgnoreCase(bankName)) {
                format = BankFormat.fromName(bankName).orElse(BankFormat.GENERIC);
            } else {
                format = bankFormatDetector.detectFromBytes(fileBytes);
            }
            
            log.info("Using bank format: {}", format.getDisplayName());
            
            // Get strategy and parse with details
            BankCsvStrategy strategy = getStrategyForFormat(format);
            
            if (strategy instanceof AbstractBankStrategy) {
                return ((AbstractBankStrategy) strategy).parseWithDetails(new ByteArrayInputStream(fileBytes));
            } else {
                // Fallback for old-style strategies
                List<Transaction> transactions = strategy.parse(new ByteArrayInputStream(fileBytes));
                return CsvParseResult.builder()
                    .detectedFormat(format)
                    .totalRows(transactions.size())
                    .successfulRows(transactions.size())
                    .transactions(transactions.stream()
                        .map(this::convertToParseTransaction)
                        .collect(Collectors.toList()))
                    .build();
            }
            
        } catch (IOException e) {
            log.error("Error parsing CSV with details", e);
            throw new RuntimeException("Error processing CSV file", e);
        }
    }
    
    /**
     * Get list of supported bank formats
     */
    public List<String> getSupportedBanks() {
        return getStrategies().keySet().stream()
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get strategy for a specific bank format
     */
    private BankCsvStrategy getStrategyForFormat(BankFormat format) {
        // Try to find by display name first
        BankCsvStrategy strategy = getStrategies().get(format.getDisplayName());
        
        if (strategy == null) {
            // Try by enum name
            strategy = getStrategies().get(format.name());
        }
        
        if (strategy == null) {
            // Fallback to generic
            strategy = getStrategies().get("Generic");
        }
        
        if (strategy == null) {
            throw new IllegalStateException("No parser found for format: " + format.getDisplayName());
        }
        
        return strategy;
    }
    
    /**
     * Convert Transaction to ParsedTransaction (for backwards compatibility)
     */
    private CsvParseResult.ParsedTransaction convertToParseTransaction(Transaction txn) {
        return CsvParseResult.ParsedTransaction.builder()
            .externalReference(txn.getExternalReference())
            .normalizedReference(txn.getNormalizedReference())
            .amount(txn.getAmount())
            .currency(txn.getCurrency())
            .timestamp(txn.getTimestamp())
            .status(txn.getStatus())
            .customerIdentifier(txn.getCustomerIdentifier())
            .rawData(txn.getRawData())
            .parseConfidence(1.0)
            .build();
    }
}
