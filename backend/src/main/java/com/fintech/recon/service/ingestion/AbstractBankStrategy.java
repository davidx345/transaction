package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import com.fintech.recon.dto.CsvParseResult;
import com.fintech.recon.dto.CsvParseResult.ParsedTransaction;
import com.fintech.recon.dto.CsvParseResult.TransactionType;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Abstract base class for bank-specific CSV parsing strategies.
 * Provides common parsing logic and utilities.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBankStrategy implements BankCsvStrategy {
    
    protected final ReferenceExtractor referenceExtractor;
    
    /**
     * Get the bank format configuration
     */
    protected abstract BankFormat getBankFormat();
    
    /**
     * Map CSV columns to their indices based on headers
     */
    protected abstract Map<String, Integer> mapColumns(String[] headers);
    
    @Override
    public List<Transaction> parse(InputStream inputStream) {
        CsvParseResult result = parseWithDetails(inputStream);
        return convertToTransactions(result);
    }
    
    /**
     * Parse CSV with full details and metadata
     */
    public CsvParseResult parseWithDetails(InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        BankFormat format = getBankFormat();
        
        CsvParseResult result = CsvParseResult.builder()
            .detectedFormat(format)
            .build();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            // Read and process header
            String[] headers = skipToHeader(reader);
            if (headers == null) {
                result.addError(0, "No valid header found", "");
                return result;
            }
            
            Map<String, Integer> columnMap = mapColumns(headers);
            validateRequiredColumns(columnMap, result);
            
            // Process data rows
            int rowNum = 1;
            String[] line;
            while ((line = reader.readNext()) != null) {
                rowNum++;
                result.setTotalRows(result.getTotalRows() + 1);
                
                try {
                    if (isEmptyRow(line)) {
                        result.setSkippedRows(result.getSkippedRows() + 1);
                        continue;
                    }
                    
                    ParsedTransaction parsed = parseRow(line, columnMap, rowNum);
                    if (parsed != null) {
                        result.addTransaction(parsed);
                    } else {
                        result.addError(rowNum, "Failed to parse row", String.join(",", line));
                    }
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", rowNum, e.getMessage());
                    result.addError(rowNum, e.getMessage(), String.join(",", line));
                }
            }
            
            // Set metadata
            result.setMetadata(CsvParseResult.ParsingMetadata.builder()
                .parseTimeMs(System.currentTimeMillis() - startTime)
                .headerRow(0)
                .delimiter(',')
                .dateFormat(format.getDateFormat())
                .autoDetected(false)
                .detectionMethod("manual")
                .detectedColumns(Arrays.asList(headers))
                .columnMapping(columnMap)
                .build());
            
        } catch (IOException | CsvValidationException e) {
            log.error("Error parsing {} CSV", format.getDisplayName(), e);
            result.addError(0, "CSV parsing error: " + e.getMessage(), "");
        }
        
        log.info("Parsed {} transactions from {} CSV ({} errors, {} skipped)", 
            result.getSuccessfulRows(), format.getDisplayName(), 
            result.getFailedRows(), result.getSkippedRows());
        
        return result;
    }
    
    /**
     * Parse a single row into a ParsedTransaction
     */
    protected ParsedTransaction parseRow(String[] line, Map<String, Integer> columnMap, int rowNum) {
        BankFormat format = getBankFormat();
        List<String> warnings = new ArrayList<>();
        
        // Extract reference
        String reference = extractColumn(line, columnMap, "REFERENCE");
        String narration = extractColumn(line, columnMap, "NARRATION");
        String normalizedRef = null;
        String refSource = "column";
        
        if (reference != null && !reference.trim().isEmpty()) {
            normalizedRef = referenceExtractor.normalizeReference(reference, format);
        } else if (narration != null && !narration.isEmpty()) {
            // Try to extract reference from narration
            Optional<String> extracted = referenceExtractor.extractReference(narration);
            if (extracted.isPresent()) {
                reference = extracted.get();
                normalizedRef = referenceExtractor.normalizeReference(reference, format);
                refSource = "narration";
            } else {
                warnings.add("No reference found in narration");
            }
        }
        
        if (normalizedRef == null || normalizedRef.isEmpty()) {
            // Generate a reference from row data
            normalizedRef = generateFallbackReference(line, rowNum);
            refSource = "generated";
            warnings.add("Reference was auto-generated");
        }
        
        // Extract amount
        BigDecimal amount = parseAmount(line, columnMap);
        TransactionType txnType = determineTransactionType(line, columnMap);
        
        // Extract date
        LocalDateTime timestamp = parseDate(line, columnMap, format);
        
        // Extract status (if available)
        String status = extractColumn(line, columnMap, "STATUS");
        if (status == null || status.isEmpty()) {
            status = "SUCCESS"; // Default for bank statements
        }
        
        // Extract customer identifier (if available)
        String customerIdentifier = extractColumn(line, columnMap, "CUSTOMER");
        if (customerIdentifier == null) {
            customerIdentifier = extractColumn(line, columnMap, "ACCOUNT");
        }
        
        // Build raw data map
        Map<String, Object> rawData = buildRawData(line, columnMap);
        
        // Calculate parse confidence
        double confidence = calculateParseConfidence(reference, amount, timestamp, refSource);
        
        return ParsedTransaction.builder()
            .externalReference(reference)
            .normalizedReference(normalizedRef)
            .amount(amount)
            .currency("NGN")
            .timestamp(timestamp)
            .status(normalizeStatus(status))
            .narration(narration)
            .customerIdentifier(customerIdentifier)
            .transactionType(txnType)
            .rawData(rawData)
            .sourceRow(rowNum)
            .extractedReferenceSource(refSource)
            .parseConfidence(confidence)
            .parseWarnings(warnings)
            .build();
    }
    
    /**
     * Extract a column value by its logical name
     */
    protected String extractColumn(String[] line, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index != null && index < line.length) {
            String value = line[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
    
    /**
     * Parse amount from debit/credit columns
     */
    protected BigDecimal parseAmount(String[] line, Map<String, Integer> columnMap) {
        // Try single amount column first
        String amountStr = extractColumn(line, columnMap, "AMOUNT");
        if (amountStr != null && !amountStr.isEmpty()) {
            return parseAmountString(amountStr);
        }
        
        // Try debit column
        String debitStr = extractColumn(line, columnMap, "DEBIT");
        if (debitStr != null && !debitStr.isEmpty()) {
            BigDecimal debit = parseAmountString(debitStr);
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                return debit;
            }
        }
        
        // Try credit column
        String creditStr = extractColumn(line, columnMap, "CREDIT");
        if (creditStr != null && !creditStr.isEmpty()) {
            BigDecimal credit = parseAmountString(creditStr);
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                return credit;
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Parse amount string handling various formats
     */
    protected BigDecimal parseAmountString(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Remove currency symbols, commas, and spaces
            String cleaned = amountStr
                .replaceAll("[₦NGN$€£]", "")
                .replaceAll(",", "")
                .replaceAll("\\s+", "")
                .replaceAll("[()]", "") // Handle negative in parentheses
                .trim();
            
            // Handle negative
            boolean isNegative = amountStr.contains("(") || amountStr.contains("-");
            cleaned = cleaned.replaceAll("-", "");
            
            if (cleaned.isEmpty()) return null;
            
            BigDecimal amount = new BigDecimal(cleaned);
            return isNegative ? amount.negate() : amount;
            
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return null;
        }
    }
    
    /**
     * Determine if transaction is debit or credit
     */
    protected TransactionType determineTransactionType(String[] line, Map<String, Integer> columnMap) {
        String debitStr = extractColumn(line, columnMap, "DEBIT");
        String creditStr = extractColumn(line, columnMap, "CREDIT");
        
        if (debitStr != null && !debitStr.isEmpty() && parseAmountString(debitStr) != null) {
            BigDecimal debit = parseAmountString(debitStr);
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                return TransactionType.DEBIT;
            }
        }
        
        if (creditStr != null && !creditStr.isEmpty() && parseAmountString(creditStr) != null) {
            BigDecimal credit = parseAmountString(creditStr);
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                return TransactionType.CREDIT;
            }
        }
        
        // Check for type column
        String typeStr = extractColumn(line, columnMap, "TYPE");
        if (typeStr != null) {
            if (typeStr.toUpperCase().contains("DEBIT") || typeStr.toUpperCase().contains("DR")) {
                return TransactionType.DEBIT;
            }
            if (typeStr.toUpperCase().contains("CREDIT") || typeStr.toUpperCase().contains("CR")) {
                return TransactionType.CREDIT;
            }
        }
        
        return TransactionType.UNKNOWN;
    }
    
    /**
     * Parse date using various formats
     */
    protected LocalDateTime parseDate(String[] line, Map<String, Integer> columnMap, BankFormat format) {
        String dateStr = extractColumn(line, columnMap, "DATE");
        if (dateStr == null) {
            dateStr = extractColumn(line, columnMap, "VALUE_DATE");
        }
        if (dateStr == null) {
            dateStr = extractColumn(line, columnMap, "TRANS_DATE");
        }
        
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        
        // Try bank-specific format first
        try {
            DateTimeFormatter formatter = format.getDateFormatter();
            if (format.getDateFormat().contains("HH")) {
                return LocalDateTime.parse(dateStr, formatter);
            } else {
                return LocalDate.parse(dateStr, formatter).atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            // Try common formats
            return tryParseDateWithFallbacks(dateStr);
        }
    }
    
    /**
     * Try parsing date with multiple format fallbacks
     */
    protected LocalDateTime tryParseDateWithFallbacks(String dateStr) {
        DateTimeFormatter[] formats = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        
        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDate.parse(dateStr, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {}
            
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        
        log.warn("Could not parse date: {}, using current time", dateStr);
        return LocalDateTime.now();
    }
    
    /**
     * Normalize status to standard format
     */
    protected String normalizeStatus(String status) {
        if (status == null) return "SUCCESS";
        
        String upper = status.toUpperCase().trim();
        
        // Success variants
        if (upper.matches(".*(" + 
            "SUCCESS|SUCCESSFUL|COMPLETED|APPROVED|PROCESSED|PAID|SETTLED|POSTED" +
            ").*")) {
            return "SUCCESS";
        }
        
        // Pending variants
        if (upper.matches(".*(" +
            "PENDING|PROCESSING|IN.?PROGRESS|AWAITING|QUEUED" +
            ").*")) {
            return "PENDING";
        }
        
        // Failed variants
        if (upper.matches(".*(" +
            "FAILED|FAILURE|DECLINED|REJECTED|CANCELLED|REVERSED|ERROR" +
            ").*")) {
            return "FAILED";
        }
        
        return status;
    }
    
    /**
     * Skip metadata rows and find header
     */
    protected String[] skipToHeader(CSVReader reader) throws IOException, CsvValidationException {
        String[] line;
        int attempts = 0;
        
        while ((line = reader.readNext()) != null && attempts < 10) {
            if (looksLikeHeader(line)) {
                return line;
            }
            attempts++;
        }
        
        return null;
    }
    
    /**
     * Check if a row looks like a header
     */
    protected boolean looksLikeHeader(String[] row) {
        String[] keywords = {"REF", "REFERENCE", "AMOUNT", "DATE", "DEBIT", "CREDIT", "TRANS"};
        String joined = String.join(" ", row).toUpperCase();
        
        int matches = 0;
        for (String keyword : keywords) {
            if (joined.contains(keyword)) matches++;
        }
        
        return matches >= 2;
    }
    
    /**
     * Check if a row is empty
     */
    protected boolean isEmptyRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Generate fallback reference when none found
     */
    protected String generateFallbackReference(String[] line, int rowNum) {
        BankFormat format = getBankFormat();
        return String.format("%s-ROW%d-%d", 
            format.name(), rowNum, System.currentTimeMillis() % 100000);
    }
    
    /**
     * Build raw data map from CSV row
     */
    protected Map<String, Object> buildRawData(String[] line, Map<String, Integer> columnMap) {
        Map<String, Object> rawData = new HashMap<>();
        for (Map.Entry<String, Integer> entry : columnMap.entrySet()) {
            if (entry.getValue() < line.length) {
                rawData.put(entry.getKey(), line[entry.getValue()]);
            }
        }
        return rawData;
    }
    
    /**
     * Calculate parsing confidence
     */
    protected double calculateParseConfidence(String reference, BigDecimal amount, 
            LocalDateTime timestamp, String refSource) {
        double confidence = 0.5;
        
        if (reference != null && !reference.isEmpty()) {
            confidence += 0.2;
            if ("column".equals(refSource)) confidence += 0.1;
        }
        
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            confidence += 0.15;
        }
        
        if (timestamp != null) {
            confidence += 0.1;
        }
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Validate required columns are present
     */
    protected void validateRequiredColumns(Map<String, Integer> columnMap, CsvParseResult result) {
        if (!columnMap.containsKey("AMOUNT") && 
            !columnMap.containsKey("DEBIT") && 
            !columnMap.containsKey("CREDIT")) {
            result.addWarning("No amount column found");
        }
        
        if (!columnMap.containsKey("DATE") && 
            !columnMap.containsKey("VALUE_DATE") &&
            !columnMap.containsKey("TRANS_DATE")) {
            result.addWarning("No date column found");
        }
    }
    
    /**
     * Convert parsed transactions to domain entities
     */
    protected List<Transaction> convertToTransactions(CsvParseResult result) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (ParsedTransaction parsed : result.getTransactions()) {
            Transaction transaction = Transaction.builder()
                .source("bank")
                .externalReference(parsed.getExternalReference())
                .normalizedReference(parsed.getNormalizedReference())
                .amount(parsed.getAmount())
                .currency(parsed.getCurrency())
                .timestamp(parsed.getTimestamp())
                .status(parsed.getStatus())
                .customerIdentifier(parsed.getCustomerIdentifier())
                .rawData(parsed.getRawData())
                .build();
            
            transactions.add(transaction);
        }
        
        return transactions;
    }
}
