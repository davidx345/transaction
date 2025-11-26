package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Zenith Bank Nigeria statements.
 * 
 * Zenith Bank statement format typically includes:
 * - TRANS DATE, VALUE DATE, TRANS ID, DR AMOUNT, CR AMOUNT, BALANCE, REMARKS
 * - Date format: dd/MM/yyyy
 * - Reference prefix: ZEN-
 */
@Component
@Slf4j
public class ZenithBankStrategy extends AbstractBankStrategy {
    
    public ZenithBankStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.ZENITH_BANK;
    }
    
    @Override
    public String getBankName() {
        return "ZenithBank";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim();
            
            // Reference columns
            if (header.contains("TRANS ID") || header.equals("TRANSACTION ID") || 
                header.contains("REFERENCE") || header.equals("REF")) {
                columnMap.put("REFERENCE", i);
            }
            
            // Date columns
            if (header.contains("TRANS DATE") || header.equals("TRANSACTION DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.contains("VALUE DATE")) {
                columnMap.put("VALUE_DATE", i);
            }
            
            // Amount columns - Zenith uses DR/CR AMOUNT format
            if (header.equals("DR AMOUNT") || header.equals("DEBIT AMOUNT") || 
                header.equals("DR") || header.equals("DEBIT")) {
                columnMap.put("DEBIT", i);
            }
            if (header.equals("CR AMOUNT") || header.equals("CREDIT AMOUNT") || 
                header.equals("CR") || header.equals("CREDIT")) {
                columnMap.put("CREDIT", i);
            }
            if (header.equals("AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            
            // Other columns
            if (header.contains("BALANCE") || header.equals("BAL")) {
                columnMap.put("BALANCE", i);
            }
            if (header.contains("REMARK") || header.contains("NARRATION") || 
                header.contains("DESCRIPTION")) {
                columnMap.put("NARRATION", i);
            }
            if (header.contains("STATUS")) {
                columnMap.put("STATUS", i);
            }
            if (header.contains("ACCOUNT") || header.equals("ACCT NO")) {
                columnMap.put("ACCOUNT", i);
            }
        }
        
        // Use trans date as primary if value date not available
        if (!columnMap.containsKey("DATE") && columnMap.containsKey("VALUE_DATE")) {
            columnMap.put("DATE", columnMap.get("VALUE_DATE"));
        }
        
        log.debug("Zenith Bank column mapping: {}", columnMap);
        return columnMap;
    }
}
