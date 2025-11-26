package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Flutterwave transaction exports.
 * 
 * Flutterwave export format typically includes:
 * - TX_REF, FLUTTERWAVEREF, AMOUNT, CHARGED_AMOUNT, CURRENCY, STATUS, CREATED_AT, etc.
 * - Date format: yyyy-MM-dd HH:mm:ss
 * - Reference prefix: FLW-
 */
@Component
@Slf4j
public class FlutterwaveStrategy extends AbstractBankStrategy {
    
    public FlutterwaveStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.FLUTTERWAVE;
    }
    
    @Override
    public String getBankName() {
        return "Flutterwave";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_");
            
            // Reference columns - Flutterwave uses TX_REF and FLUTTERWAVEREF
            if (header.equals("TX_REF") || header.equals("TRANSACTION_REF") || 
                header.equals("REFERENCE")) {
                columnMap.put("REFERENCE", i);
            }
            if (header.equals("FLUTTERWAVEREF") || header.equals("FLW_REF") || 
                header.equals("FLUTTERWAVE_REF")) {
                columnMap.put("FLW_REF", i);
            }
            if (header.equals("ID") || header.equals("TRANSACTION_ID")) {
                columnMap.put("TRANSACTION_ID", i);
            }
            
            // Date columns
            if (header.equals("CREATED_AT") || header.equals("DATE") || 
                header.equals("TRANSACTION_DATE")) {
                columnMap.put("DATE", i);
            }
            
            // Amount columns
            if (header.equals("AMOUNT") || header.equals("ACTUAL_AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("CHARGED_AMOUNT") || header.equals("TOTAL_AMOUNT")) {
                columnMap.put("CHARGED_AMOUNT", i);
            }
            if (header.equals("APP_FEE") || header.equals("FEE") || header.equals("FLUTTERWAVE_FEE")) {
                columnMap.put("FEE", i);
            }
            if (header.equals("MERCHANT_FEE")) {
                columnMap.put("MERCHANT_FEE", i);
            }
            
            // Currency
            if (header.equals("CURRENCY") || header.equals("TRANSACTION_CURRENCY")) {
                columnMap.put("CURRENCY", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("TRANSACTION_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Customer info
            if (header.equals("CUSTOMER_EMAIL") || header.equals("EMAIL")) {
                columnMap.put("CUSTOMER", i);
            }
            if (header.equals("CUSTOMER_NAME") || header.equals("NAME")) {
                columnMap.put("CUSTOMER_NAME", i);
            }
            if (header.equals("CUSTOMER_PHONE") || header.equals("PHONE")) {
                columnMap.put("CUSTOMER_PHONE", i);
            }
            
            // Payment details
            if (header.equals("PAYMENT_TYPE") || header.equals("CHANNEL")) {
                columnMap.put("CHANNEL", i);
            }
            if (header.equals("ACCOUNT_ID")) {
                columnMap.put("ACCOUNT", i);
            }
            if (header.contains("NARRATION") || header.contains("DESCRIPTION")) {
                columnMap.put("NARRATION", i);
            }
        }
        
        // Use tx_ref if no reference found
        if (!columnMap.containsKey("REFERENCE") && columnMap.containsKey("FLW_REF")) {
            columnMap.put("REFERENCE", columnMap.get("FLW_REF"));
        }
        
        log.debug("Flutterwave column mapping: {}", columnMap);
        return columnMap;
    }
}
