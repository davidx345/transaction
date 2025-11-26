package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Paystack transaction exports.
 * 
 * Paystack export format typically includes:
 * - ID, REFERENCE, AMOUNT, CURRENCY, STATUS, PAID_AT, CREATED_AT, CUSTOMER_EMAIL, etc.
 * - Date format: yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd HH:mm:ss
 * - Reference prefix: PSK_ or PS-
 */
@Component
@Slf4j
public class PaystackStrategy extends AbstractBankStrategy {
    
    public PaystackStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.PAYSTACK;
    }
    
    @Override
    public String getBankName() {
        return "Paystack";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_");
            
            // Reference columns
            if (header.equals("REFERENCE") || header.equals("PAYMENT_REFERENCE") || 
                header.equals("PAYSTACK_REF") || header.equals("ID")) {
                if (!columnMap.containsKey("REFERENCE")) {
                    columnMap.put("REFERENCE", i);
                }
            }
            
            // Date columns - Paystack uses PAID_AT for successful payments
            if (header.equals("PAID_AT") || header.equals("PAID")) {
                columnMap.put("DATE", i);
            }
            if (header.equals("CREATED_AT") || header.equals("CREATED")) {
                columnMap.put("CREATED_DATE", i);
            }
            
            // Amount columns
            if (header.equals("AMOUNT") || header.equals("NET_AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("CHARGED_AMOUNT") || header.equals("GROSS_AMOUNT")) {
                columnMap.put("GROSS_AMOUNT", i);
            }
            if (header.equals("FEE") || header.equals("FEES") || header.equals("PAYSTACK_FEE")) {
                columnMap.put("FEE", i);
            }
            
            // Currency
            if (header.equals("CURRENCY")) {
                columnMap.put("CURRENCY", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("PAYMENT_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Customer info
            if (header.contains("CUSTOMER") || header.equals("EMAIL") || 
                header.equals("CUSTOMER_EMAIL") || header.equals("CUSTOMER_ID")) {
                if (!columnMap.containsKey("CUSTOMER")) {
                    columnMap.put("CUSTOMER", i);
                }
            }
            
            // Other useful columns
            if (header.equals("CHANNEL") || header.equals("PAYMENT_CHANNEL")) {
                columnMap.put("CHANNEL", i);
            }
            if (header.equals("AUTHORIZATION") || header.equals("AUTH_CODE")) {
                columnMap.put("AUTH_CODE", i);
            }
            if (header.equals("IP_ADDRESS") || header.equals("IP")) {
                columnMap.put("IP", i);
            }
        }
        
        // Use created_at as fallback for date
        if (!columnMap.containsKey("DATE") && columnMap.containsKey("CREATED_DATE")) {
            columnMap.put("DATE", columnMap.get("CREATED_DATE"));
        }
        
        log.debug("Paystack column mapping: {}", columnMap);
        return columnMap;
    }
}
