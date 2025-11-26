package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic CSV parser strategy for unknown bank formats.
 * Uses intelligent column detection to handle various CSV layouts.
 */
@Component
@Slf4j
public class GenericBankStrategy extends AbstractBankStrategy {
    
    public GenericBankStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.GENERIC;
    }
    
    @Override
    public String getBankName() {
        return "Generic";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        // Lists of potential column names for each field type
        String[] refKeywords = {"REFERENCE", "REF", "ID", "TRANS_ID", "TRANSACTION_ID", "TRAN_REF", "TXN_REF"};
        String[] dateKeywords = {"DATE", "TRANS_DATE", "TRANSACTION_DATE", "VALUE_DATE", "POST_DATE", "CREATED_AT", "PAID_AT"};
        String[] amountKeywords = {"AMOUNT", "TOTAL", "SUM", "VALUE"};
        String[] debitKeywords = {"DEBIT", "DR", "WITHDRAWAL", "DEBIT_AMOUNT", "DR_AMOUNT"};
        String[] creditKeywords = {"CREDIT", "CR", "DEPOSIT", "CREDIT_AMOUNT", "CR_AMOUNT"};
        String[] narrationKeywords = {"NARRATION", "DESCRIPTION", "REMARKS", "DETAILS", "PARTICULARS", "MEMO"};
        String[] statusKeywords = {"STATUS", "STATE", "RESULT", "PAYMENT_STATUS"};
        String[] customerKeywords = {"CUSTOMER", "EMAIL", "ACCOUNT", "ACCT", "CUSTOMER_ID", "ACCOUNT_NO"};
        
        for (int i = 0; i < headers.length; i++) {
            String header = normalizeHeader(headers[i]);
            
            // Reference
            if (!columnMap.containsKey("REFERENCE")) {
                for (String keyword : refKeywords) {
                    if (header.contains(keyword)) {
                        columnMap.put("REFERENCE", i);
                        break;
                    }
                }
            }
            
            // Date (prefer transaction date over others)
            for (String keyword : dateKeywords) {
                if (header.contains(keyword)) {
                    if (!columnMap.containsKey("DATE")) {
                        columnMap.put("DATE", i);
                    } else if (header.contains("TRANS") || header.contains("VALUE")) {
                        // Prefer transaction date
                        columnMap.put("DATE", i);
                    }
                    break;
                }
            }
            
            // Single amount column
            for (String keyword : amountKeywords) {
                if (header.equals(keyword)) {
                    columnMap.put("AMOUNT", i);
                    break;
                }
            }
            
            // Debit column
            for (String keyword : debitKeywords) {
                if (header.contains(keyword)) {
                    columnMap.put("DEBIT", i);
                    break;
                }
            }
            
            // Credit column
            for (String keyword : creditKeywords) {
                if (header.contains(keyword)) {
                    columnMap.put("CREDIT", i);
                    break;
                }
            }
            
            // Narration
            for (String keyword : narrationKeywords) {
                if (header.contains(keyword)) {
                    columnMap.put("NARRATION", i);
                    break;
                }
            }
            
            // Status
            for (String keyword : statusKeywords) {
                if (header.contains(keyword)) {
                    columnMap.put("STATUS", i);
                    break;
                }
            }
            
            // Customer
            for (String keyword : customerKeywords) {
                if (header.contains(keyword)) {
                    if (!columnMap.containsKey("CUSTOMER")) {
                        columnMap.put("CUSTOMER", i);
                    }
                    break;
                }
            }
            
            // Balance (for reference)
            if (header.contains("BALANCE") || header.equals("BAL")) {
                columnMap.put("BALANCE", i);
            }
        }
        
        // If no specific columns found, try positional detection
        if (columnMap.isEmpty() && headers.length >= 3) {
            log.warn("Using positional column detection for generic CSV");
            // Assume: Reference, Amount, Date (common order)
            columnMap.put("REFERENCE", 0);
            columnMap.put("AMOUNT", 1);
            columnMap.put("DATE", 2);
            if (headers.length > 3) {
                columnMap.put("STATUS", 3);
            }
        }
        
        log.debug("Generic column mapping: {}", columnMap);
        return columnMap;
    }
    
    /**
     * Normalize header for comparison
     */
    private String normalizeHeader(String header) {
        return header
            .toUpperCase()
            .trim()
            .replace(" ", "_")
            .replace("-", "_")
            .replaceAll("[^A-Z0-9_]", "");
    }
}
