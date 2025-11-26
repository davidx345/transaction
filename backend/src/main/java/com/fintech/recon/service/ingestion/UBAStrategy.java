package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for United Bank for Africa (UBA) statements.
 * 
 * UBA statement format typically includes:
 * - TRAN DATE, VALUE DATE, TRAN REF, DEBIT, CREDIT, BALANCE, NARRATION
 * - Date format: dd/MM/yyyy
 * - Reference prefix: UBA-
 */
@Component
@Slf4j
public class UBAStrategy extends AbstractBankStrategy {
    
    public UBAStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.UBA;
    }
    
    @Override
    public String getBankName() {
        return "UBA";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim();
            
            // Reference columns - UBA uses TRAN REF
            if (header.contains("TRAN REF") || header.contains("TRANS REF") || 
                header.equals("REFERENCE") || header.equals("REF NO") || header.equals("REF")) {
                columnMap.put("REFERENCE", i);
            }
            
            // Date columns - UBA uses TRAN DATE
            if (header.contains("TRAN DATE") || header.contains("TRANS DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.contains("VALUE DATE")) {
                columnMap.put("VALUE_DATE", i);
            }
            
            // Amount columns - UBA uses DR/CR or DEBIT/CREDIT
            if (header.equals("DEBIT") || header.equals("DR") || header.equals("WITHDRAWAL")) {
                columnMap.put("DEBIT", i);
            }
            if (header.equals("CREDIT") || header.equals("CR") || header.equals("DEPOSIT")) {
                columnMap.put("CREDIT", i);
            }
            if (header.equals("AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            
            // Other columns
            if (header.contains("BALANCE") || header.equals("BAL") || header.equals("LEDGER BAL")) {
                columnMap.put("BALANCE", i);
            }
            if (header.contains("NARRATION") || header.contains("DESCRIPTION") || 
                header.contains("REMARK") || header.contains("DETAILS")) {
                columnMap.put("NARRATION", i);
            }
            if (header.contains("STATUS")) {
                columnMap.put("STATUS", i);
            }
            if (header.contains("ACCOUNT") || header.equals("ACCT") || header.equals("A/C NO")) {
                columnMap.put("ACCOUNT", i);
            }
        }
        
        // Use tran date as primary if value date not available
        if (!columnMap.containsKey("DATE") && columnMap.containsKey("VALUE_DATE")) {
            columnMap.put("DATE", columnMap.get("VALUE_DATE"));
        }
        
        log.debug("UBA column mapping: {}", columnMap);
        return columnMap;
    }
}
