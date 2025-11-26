package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Guaranty Trust Bank (GTBank) Nigeria statements.
 * 
 * GTBank statement format typically includes:
 * - PAYMENT_REF/TRANS REF, AMOUNT, SETTLEMENT_DATE/TXN DATE, STATUS
 * - Date format: dd/MM/yyyy
 * - Reference prefix: GTB-
 */
@Component
@Slf4j
public class GtBankStrategy extends AbstractBankStrategy {
    
    public GtBankStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.GTBANK;
    }

    @Override
    public String getBankName() {
        return "GTBank";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim();
            
            // Reference columns
            if (header.contains("PAYMENT_REF") || header.contains("PAYMENT REF") ||
                header.contains("TRANS REF") || header.equals("REFERENCE") || 
                header.equals("TRANSACTION REF")) {
                columnMap.put("REFERENCE", i);
            }
            
            // Date columns
            if (header.contains("SETTLEMENT_DATE") || header.contains("SETTLEMENT DATE") ||
                header.contains("TXN DATE") || header.equals("DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.contains("VALUE DATE")) {
                columnMap.put("VALUE_DATE", i);
            }
            
            // Amount columns
            if (header.equals("AMOUNT") || header.equals("TRANSACTION AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("DEBIT") || header.equals("DR")) {
                columnMap.put("DEBIT", i);
            }
            if (header.equals("CREDIT") || header.equals("CR")) {
                columnMap.put("CREDIT", i);
            }
            
            // Other columns
            if (header.contains("STATUS")) {
                columnMap.put("STATUS", i);
            }
            if (header.contains("NARRATION") || header.contains("DESCRIPTION") || 
                header.contains("REMARK")) {
                columnMap.put("NARRATION", i);
            }
            if (header.contains("BALANCE")) {
                columnMap.put("BALANCE", i);
            }
            if (header.contains("ACCOUNT")) {
                columnMap.put("ACCOUNT", i);
            }
        }
        
        // Fallback for simple format: PAYMENT_REF,AMOUNT,SETTLEMENT_DATE,STATUS
        if (columnMap.isEmpty() && headers.length >= 4) {
            columnMap.put("REFERENCE", 0);
            columnMap.put("AMOUNT", 1);
            columnMap.put("DATE", 2);
            columnMap.put("STATUS", 3);
        }
        
        log.debug("GTBank column mapping: {}", columnMap);
        return columnMap;
    }
}
