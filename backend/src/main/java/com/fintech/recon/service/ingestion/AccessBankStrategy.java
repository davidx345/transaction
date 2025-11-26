package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Access Bank Nigeria statements.
 * 
 * Access Bank statement format typically includes:
 * - POST DATE, VALUE DATE, REFERENCE, DEBIT AMT, CREDIT AMT, BALANCE, NARRATION
 * - Date format: dd-MMM-yyyy (e.g., 22-Nov-2024)
 * - Reference prefix: ACC-
 */
@Component
@Slf4j
public class AccessBankStrategy extends AbstractBankStrategy {
    
    public AccessBankStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.ACCESS_BANK;
    }
    
    @Override
    public String getBankName() {
        return "AccessBank";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim();
            
            // Reference columns
            if (header.contains("REFERENCE") || header.equals("REF NO") || header.equals("REF")) {
                columnMap.put("REFERENCE", i);
            }
            
            // Date columns
            if (header.contains("POST DATE") || header.equals("POST_DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.contains("VALUE DATE") || header.equals("VALUE_DATE")) {
                columnMap.put("VALUE_DATE", i);
            }
            if (header.contains("TRANS DATE") || header.equals("TRANSACTION DATE")) {
                columnMap.put("TRANS_DATE", i);
            }
            
            // Amount columns
            if (header.contains("DEBIT") || header.equals("DR") || header.equals("DEBIT AMT")) {
                columnMap.put("DEBIT", i);
            }
            if (header.contains("CREDIT") || header.equals("CR") || header.equals("CREDIT AMT")) {
                columnMap.put("CREDIT", i);
            }
            if (header.equals("AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            
            // Other columns
            if (header.contains("BALANCE") || header.equals("BAL")) {
                columnMap.put("BALANCE", i);
            }
            if (header.contains("NARRATION") || header.contains("DESCRIPTION") || header.contains("REMARK")) {
                columnMap.put("NARRATION", i);
            }
            if (header.contains("STATUS")) {
                columnMap.put("STATUS", i);
            }
            if (header.contains("ACCOUNT") || header.equals("ACCT")) {
                columnMap.put("ACCOUNT", i);
            }
        }
        
        // Use post date as primary if available
        if (!columnMap.containsKey("DATE") && columnMap.containsKey("VALUE_DATE")) {
            columnMap.put("DATE", columnMap.get("VALUE_DATE"));
        }
        
        log.debug("Access Bank column mapping: {}", columnMap);
        return columnMap;
    }
}
