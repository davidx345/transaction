package com.fintech.recon.dto;

import com.fintech.recon.service.ingestion.BankFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the result of parsing a bank CSV file.
 * Contains parsed transactions along with metadata about the parsing process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvParseResult {
    
    private BankFormat detectedFormat;
    private String fileName;
    private int totalRows;
    private int successfulRows;
    private int failedRows;
    private int skippedRows;
    
    @Builder.Default
    private List<ParsedTransaction> transactions = new ArrayList<>();
    
    @Builder.Default
    private List<ParseError> errors = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    private ParsingMetadata metadata;
    
    public void addTransaction(ParsedTransaction transaction) {
        transactions.add(transaction);
        successfulRows++;
    }
    
    public void addError(int row, String message, String rawData) {
        errors.add(new ParseError(row, message, rawData));
        failedRows++;
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public double getSuccessRate() {
        if (totalRows == 0) return 0;
        return (double) successfulRows / totalRows * 100;
    }
    
    /**
     * Parsed transaction with additional metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedTransaction {
        private String externalReference;
        private String normalizedReference;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime timestamp;
        private String status;
        private String narration;
        private String customerIdentifier;
        private TransactionType transactionType;
        private Map<String, Object> rawData;
        
        // Parsing metadata
        private int sourceRow;
        private String extractedReferenceSource; // "column", "narration", "combined"
        private double parseConfidence; // 0-1
        
        @Builder.Default
        private List<String> parseWarnings = new ArrayList<>();
    }
    
    /**
     * Transaction type (debit vs credit)
     */
    public enum TransactionType {
        DEBIT,
        CREDIT,
        UNKNOWN
    }
    
    /**
     * Error during parsing
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParseError {
        private int row;
        private String message;
        private String rawData;
    }
    
    /**
     * Metadata about the parsing process
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsingMetadata {
        private long parseTimeMs;
        private int headerRow;
        private char delimiter;
        private String dateFormat;
        private boolean autoDetected;
        private String detectionMethod; // "header", "reference", "content", "manual"
        
        @Builder.Default
        private List<String> detectedColumns = new ArrayList<>();
        
        private Map<String, Integer> columnMapping;
    }
}
