package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for First Bank of Nigeria statements.
 * 
 * First Bank statement format typically includes:
 * - TRANSACTION DATE, VALUE DATE, REFERENCE, DEBIT, CREDIT, BALANCE, DESCRIPTION
 * - Date format: yyyy-MM-dd (ISO format)
 * - Reference prefix: FBN-
 */
@Component
@Slf4j
public class FirstBankStrategy extends AbstractBankStrategy {
    
    public FirstBankStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.FIRST_BANK;
    }
    
    @Override
    public String getBankName() {
        return "FirstBank";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim();
            
            // Reference columns
            if (header.contains("REFERENCE") || header.equals("TRANS REF") || 
                header.equals("TRANSACTION REFERENCE") || header.equals("REF")) {
                columnMap.put("REFERENCE", i);
            }
            
            // Date columns - First Bank typically uses TRANSACTION DATE
            if (header.contains("TRANSACTION DATE") || header.equals("TRANS DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.contains("VALUE DATE")) {
                columnMap.put("VALUE_DATE", i);
            }
            if (header.contains("POST DATE")) {
                columnMap.put("POST_DATE", i);
            }
            
            // Amount columns
            if (header.equals("DEBIT") || header.equals("DR") || header.equals("DEBIT AMOUNT")) {
                columnMap.put("DEBIT", i);
            }
            if (header.equals("CREDIT") || header.equals("CR") || header.equals("CREDIT AMOUNT")) {
                columnMap.put("CREDIT", i);
            }
            if (header.equals("AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            
            // Other columns
            if (header.contains("BALANCE") || header.equals("BAL") || header.equals("LEDGER BALANCE")) {
                columnMap.put("BALANCE", i);
            }
            if (header.contains("DESCRIPTION") || header.contains("NARRATION") || 
                header.contains("PARTICULARS")) {
                columnMap.put("NARRATION", i);
            }
            if (header.contains("STATUS")) {
                columnMap.put("STATUS", i);
            }
            if (header.contains("ACCOUNT") || header.equals("ACCT") || header.equals("ACCOUNT NO")) {
                columnMap.put("ACCOUNT", i);
            }
            if (header.contains("BRANCH")) {
                columnMap.put("BRANCH", i);
            }
        }
        
        // Fallback date column
        if (!columnMap.containsKey("DATE")) {
            if (columnMap.containsKey("VALUE_DATE")) {
                columnMap.put("DATE", columnMap.get("VALUE_DATE"));
            } else if (columnMap.containsKey("POST_DATE")) {
                columnMap.put("DATE", columnMap.get("POST_DATE"));
            }
        }
        
        log.debug("First Bank column mapping: {}", columnMap);
        return columnMap;
    }
}
