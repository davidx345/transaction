package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Korapay transaction exports.
 * 
 * Korapay export format typically includes:
 * - REFERENCE, AMOUNT, FEE, CURRENCY, STATUS, CREATED_AT, CUSTOMER_EMAIL, etc.
 * - Date format: yyyy-MM-dd'T'HH:mm:ss
 * - Reference prefix: KPY-
 * 
 * Supported transaction types:
 * - Card payments
 * - Bank transfers
 * - Mobile money
 * - Virtual bank accounts
 */
@Component
@Slf4j
public class KorapayStrategy extends AbstractBankStrategy {
    
    public KorapayStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.KORAPAY;
    }
    
    @Override
    public String getBankName() {
        return "Korapay";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_").replace("-", "_");
            
            // Reference columns - Korapay uses various reference formats
            if (header.equals("REFERENCE") || header.equals("KORAPAY_REF") || 
                header.equals("PAYMENT_REFERENCE") || header.equals("TRANSACTION_REFERENCE") ||
                header.equals("ID")) {
                if (!columnMap.containsKey("REFERENCE")) {
                    columnMap.put("REFERENCE", i);
                }
            }
            
            // Date columns
            if (header.equals("CREATED_AT") || header.equals("CREATED")) {
                columnMap.put("DATE", i);
            }
            if (header.equals("PAID_AT") || header.equals("COMPLETED_AT")) {
                columnMap.put("PAID_DATE", i);
            }
            if (header.equals("TIMESTAMP") || header.equals("DATE")) {
                if (!columnMap.containsKey("DATE")) {
                    columnMap.put("DATE", i);
                }
            }
            
            // Amount columns
            if (header.equals("AMOUNT") || header.equals("CHARGED_AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("NET_AMOUNT") || header.equals("SETTLED_AMOUNT")) {
                columnMap.put("NET_AMOUNT", i);
            }
            if (header.equals("FEE") || header.equals("FEES") || header.equals("KORAPAY_FEE")) {
                columnMap.put("FEE", i);
            }
            
            // Currency
            if (header.equals("CURRENCY")) {
                columnMap.put("CURRENCY", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("PAYMENT_STATUS") || 
                header.equals("TRANSACTION_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Customer info
            if (header.contains("CUSTOMER") || header.equals("EMAIL") || 
                header.equals("CUSTOMER_EMAIL") || header.equals("CUSTOMER_ID") ||
                header.equals("CUSTOMER_NAME")) {
                if (!columnMap.containsKey("CUSTOMER")) {
                    columnMap.put("CUSTOMER", i);
                }
            }
            
            // Narration / Description
            if (header.equals("NARRATION") || header.equals("DESCRIPTION") || 
                header.equals("MEMO") || header.equals("REMARK")) {
                columnMap.put("NARRATION", i);
            }
            
            // Payment channel/type
            if (header.equals("CHANNEL") || header.equals("PAYMENT_CHANNEL") ||
                header.equals("PAYMENT_TYPE") || header.equals("TYPE")) {
                columnMap.put("CHANNEL", i);
            }
            
            // Bank/account info for bank transfers
            if (header.equals("BANK_NAME") || header.equals("BANK")) {
                columnMap.put("BANK", i);
            }
            if (header.equals("ACCOUNT_NUMBER") || header.equals("ACCOUNT_NO")) {
                columnMap.put("ACCOUNT", i);
            }
        }
        
        // Use paid_at as primary date if available (more accurate for settlements)
        if (columnMap.containsKey("PAID_DATE") && !columnMap.containsKey("DATE")) {
            columnMap.put("DATE", columnMap.get("PAID_DATE"));
        }
        
        log.debug("Korapay column mapping: {}", columnMap);
        return columnMap;
    }
}
