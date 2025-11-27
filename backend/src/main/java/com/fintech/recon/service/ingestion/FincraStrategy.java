package com.fintech.recon.service.ingestion;

import com.fintech.recon.dto.CsvParseResult.ParsedTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Fincra transaction exports.
 * 
 * Fincra is a cross-border payments infrastructure provider - export format includes:
 * - REFERENCE, SOURCE_AMOUNT, DESTINATION_AMOUNT, SOURCE_CURRENCY, DESTINATION_CURRENCY, etc.
 * - Date format: yyyy-MM-dd'T'HH:mm:ss
 * - Reference prefix: FNC-
 * 
 * Supported transaction types:
 * - Collections (local and international)
 * - Payouts (local and international)
 * - Virtual accounts
 * - Conversions (FX)
 */
@Component
@Slf4j
public class FincraStrategy extends AbstractBankStrategy {
    
    public FincraStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.FINCRA;
    }
    
    @Override
    public String getBankName() {
        return "Fincra";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_").replace("-", "_");
            
            // Reference columns - Fincra uses session_id for tracking
            if (header.equals("REFERENCE") || header.equals("SESSION_ID") || 
                header.equals("FINCRA_REF") || header.equals("TRANSACTION_REFERENCE") ||
                header.equals("ID") || header.equals("PAYMENT_REFERENCE")) {
                if (!columnMap.containsKey("REFERENCE")) {
                    columnMap.put("REFERENCE", i);
                }
            }
            
            // Date columns
            if (header.equals("CREATED_AT") || header.equals("CREATED")) {
                columnMap.put("DATE", i);
            }
            if (header.equals("COMPLETED_AT") || header.equals("SETTLED_AT")) {
                columnMap.put("COMPLETED_DATE", i);
            }
            
            // Amount columns - Fincra has source and destination amounts for FX
            if (header.equals("AMOUNT") || header.equals("SOURCE_AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("DESTINATION_AMOUNT") || header.equals("SETTLEMENT_AMOUNT")) {
                columnMap.put("DEST_AMOUNT", i);
            }
            if (header.equals("FEE") || header.equals("FEES") || header.equals("CHARGE")) {
                columnMap.put("FEE", i);
            }
            
            // Currency columns - Fincra handles multi-currency
            if (header.equals("CURRENCY") || header.equals("SOURCE_CURRENCY")) {
                columnMap.put("CURRENCY", i);
            }
            if (header.equals("DESTINATION_CURRENCY") || header.equals("SETTLEMENT_CURRENCY")) {
                columnMap.put("DEST_CURRENCY", i);
            }
            
            // Exchange rate
            if (header.equals("RATE") || header.equals("EXCHANGE_RATE") || 
                header.equals("FX_RATE")) {
                columnMap.put("RATE", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("PAYMENT_STATUS") ||
                header.equals("TRANSACTION_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Transaction type (collection, payout, conversion)
            if (header.equals("TYPE") || header.equals("TRANSACTION_TYPE") ||
                header.equals("PAYMENT_TYPE")) {
                columnMap.put("TYPE", i);
            }
            
            // Customer/Beneficiary info
            if (header.contains("CUSTOMER") || header.equals("EMAIL") || 
                header.equals("CUSTOMER_EMAIL")) {
                if (!columnMap.containsKey("CUSTOMER")) {
                    columnMap.put("CUSTOMER", i);
                }
            }
            if (header.contains("BENEFICIARY") || header.equals("RECIPIENT")) {
                columnMap.put("BENEFICIARY", i);
            }
            
            // Narration / Description
            if (header.equals("NARRATION") || header.equals("DESCRIPTION") || 
                header.equals("REASON") || header.equals("PURPOSE")) {
                columnMap.put("NARRATION", i);
            }
            
            // Bank details for payouts
            if (header.equals("BANK_NAME") || header.equals("BANK") ||
                header.equals("DESTINATION_BANK")) {
                columnMap.put("BANK", i);
            }
            if (header.equals("ACCOUNT_NUMBER") || header.equals("ACCOUNT_NO") ||
                header.equals("DESTINATION_ACCOUNT")) {
                columnMap.put("ACCOUNT", i);
            }
            
            // Country for international transfers
            if (header.equals("COUNTRY") || header.equals("DESTINATION_COUNTRY") ||
                header.equals("SOURCE_COUNTRY")) {
                if (!columnMap.containsKey("COUNTRY")) {
                    columnMap.put("COUNTRY", i);
                }
            }
        }
        
        // Use completed date as primary if available
        if (columnMap.containsKey("COMPLETED_DATE") && !columnMap.containsKey("DATE")) {
            columnMap.put("DATE", columnMap.get("COMPLETED_DATE"));
        }
        
        log.debug("Fincra column mapping: {}", columnMap);
        return columnMap;
    }
    
    @Override
    protected String normalizeStatus(String status) {
        if (status == null) return "SUCCESS";
        
        String upper = status.toUpperCase().trim();
        
        // Fincra-specific statuses
        if (upper.matches(".*(SUCCESSFUL|COMPLETED|SETTLED|PAID|DONE).*")) {
            return "SUCCESS";
        }
        if (upper.matches(".*(PENDING|PROCESSING|AWAITING|INITIATED|NEW).*")) {
            return "PENDING";
        }
        if (upper.matches(".*(FAILED|REJECTED|CANCELLED|REVERSED|EXPIRED|DECLINED).*")) {
            return "FAILED";
        }
        
        return super.normalizeStatus(status);
    }
    
    @Override
    protected ParsedTransaction parseRow(String[] line, Map<String, Integer> columnMap, int rowNum) {
        ParsedTransaction parsed = super.parseRow(line, columnMap, rowNum);
        
        if (parsed != null) {
            // Add Fincra-specific multi-currency data to raw data
            Map<String, Object> rawData = parsed.getRawData();
            
            String destAmount = extractColumn(line, columnMap, "DEST_AMOUNT");
            String destCurrency = extractColumn(line, columnMap, "DEST_CURRENCY");
            String rate = extractColumn(line, columnMap, "RATE");
            
            if (destAmount != null) {
                rawData.put("destinationAmount", destAmount);
            }
            if (destCurrency != null) {
                rawData.put("destinationCurrency", destCurrency);
            }
            if (rate != null) {
                rawData.put("exchangeRate", rate);
            }
        }
        
        return parsed;
    }
}
