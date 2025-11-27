package com.fintech.recon.service.ingestion;

import com.fintech.recon.dto.CsvParseResult.ParsedTransaction;
import com.fintech.recon.dto.CsvParseResult.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Moniepoint/Monnify transaction exports.
 * 
 * Moniepoint (formerly TeamApt/Monnify) export format typically includes:
 * - REFERENCE, AMOUNT, STATUS, PAID_ON, CUSTOMER_NAME, etc.
 * - Date format: yyyy-MM-dd'T'HH:mm:ss
 * - Reference prefix: MNFY- or MNP-
 * 
 * Supported transaction types:
 * - Collections (bank transfer, card, USSD)
 * - Disbursements/Payouts
 * - Virtual accounts
 * - POS transactions
 * - Agency banking
 */
@Component
@Slf4j
public class MoniepointStrategy extends AbstractBankStrategy {
    
    public MoniepointStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.MONIEPOINT;
    }
    
    @Override
    public String getBankName() {
        return "Moniepoint";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_").replace("-", "_");
            
            // Reference columns - Monnify uses various reference formats
            if (header.equals("REFERENCE") || header.equals("TRANSACTION_REFERENCE") || 
                header.equals("PAYMENT_REFERENCE") || header.equals("MONNIFY_REF") ||
                header.equals("MONIEPOINT_REF") || header.equals("ID")) {
                if (!columnMap.containsKey("REFERENCE")) {
                    columnMap.put("REFERENCE", i);
                }
            }
            
            // Transaction hash (unique to Monnify webhook verification)
            if (header.equals("TRANSACTION_HASH") || header.equals("HASH")) {
                columnMap.put("TRANSACTION_HASH", i);
            }
            
            // Date columns
            if (header.equals("PAID_ON") || header.equals("PAYMENT_DATE")) {
                columnMap.put("DATE", i);
            }
            if (header.equals("TRANSACTION_DATE") || header.equals("TRANS_DATE")) {
                columnMap.put("TRANS_DATE", i);
            }
            if (header.equals("CREATED_ON") || header.equals("CREATED_AT")) {
                columnMap.put("CREATED_DATE", i);
            }
            
            // Amount columns
            if (header.equals("AMOUNT") || header.equals("AMOUNT_PAID") ||
                header.equals("TRANSACTION_AMOUNT")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("SETTLED_AMOUNT") || header.equals("NET_AMOUNT")) {
                columnMap.put("SETTLED_AMOUNT", i);
            }
            if (header.equals("FEE") || header.equals("CHARGES") || 
                header.equals("COMMISSION") || header.equals("MONNIFY_FEE")) {
                columnMap.put("FEE", i);
            }
            
            // Currency
            if (header.equals("CURRENCY") || header.equals("CURRENCY_CODE")) {
                columnMap.put("CURRENCY", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("PAYMENT_STATUS") ||
                header.equals("TRANSACTION_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Payment method/channel
            if (header.equals("PAYMENT_METHOD") || header.equals("PAYMENT_SOURCE") ||
                header.equals("CHANNEL") || header.equals("SOURCE_TYPE")) {
                columnMap.put("CHANNEL", i);
            }
            
            // Product type (reserved_account, card, transfer, pos, ussd)
            if (header.equals("PRODUCT") || header.equals("PRODUCT_TYPE") ||
                header.equals("COLLECTION_TYPE")) {
                columnMap.put("PRODUCT", i);
            }
            
            // Customer info
            if (header.contains("CUSTOMER") || header.equals("CUSTOMER_NAME") || 
                header.equals("CUSTOMER_EMAIL") || header.equals("PAYER_NAME")) {
                if (!columnMap.containsKey("CUSTOMER")) {
                    columnMap.put("CUSTOMER", i);
                }
            }
            if (header.equals("PAYER_BANK") || header.equals("CUSTOMER_BANK")) {
                columnMap.put("PAYER_BANK", i);
            }
            if (header.equals("PAYER_BANK_ACCOUNT") || header.equals("SOURCE_ACCOUNT")) {
                columnMap.put("PAYER_ACCOUNT", i);
            }
            
            // Narration / Description
            if (header.equals("NARRATION") || header.equals("DESCRIPTION") || 
                header.equals("PAYMENT_DESCRIPTION")) {
                columnMap.put("NARRATION", i);
            }
            
            // Destination account (for collections)
            if (header.equals("DESTINATION_ACCOUNT") || header.equals("ACCOUNT_NUMBER") ||
                header.equals("VIRTUAL_ACCOUNT")) {
                columnMap.put("ACCOUNT", i);
            }
            if (header.equals("DESTINATION_BANK") || header.equals("BANK_NAME")) {
                columnMap.put("BANK", i);
            }
            
            // Card details (if card payment)
            if (header.equals("CARD_TYPE") || header.equals("CARD_BRAND")) {
                columnMap.put("CARD_TYPE", i);
            }
            if (header.equals("MASKED_PAN") || header.equals("CARD_NUMBER")) {
                columnMap.put("MASKED_PAN", i);
            }
            
            // POS-specific columns
            if (header.equals("TERMINAL_ID") || header.equals("TERMINAL")) {
                columnMap.put("TERMINAL_ID", i);
            }
            if (header.equals("RRN") || header.equals("RETRIEVAL_REFERENCE")) {
                columnMap.put("RRN", i);
            }
        }
        
        // Date fallbacks
        if (!columnMap.containsKey("DATE")) {
            if (columnMap.containsKey("TRANS_DATE")) {
                columnMap.put("DATE", columnMap.get("TRANS_DATE"));
            } else if (columnMap.containsKey("CREATED_DATE")) {
                columnMap.put("DATE", columnMap.get("CREATED_DATE"));
            }
        }
        
        log.debug("Moniepoint column mapping: {}", columnMap);
        return columnMap;
    }
    
    @Override
    protected TransactionType determineTransactionType(String[] line, Map<String, Integer> columnMap) {
        // Check PRODUCT column for transaction type
        String productStr = extractColumn(line, columnMap, "PRODUCT");
        if (productStr != null) {
            String upper = productStr.toUpperCase();
            // Collections are credits to the merchant
            if (upper.contains("COLLECTION") || upper.contains("RESERVED_ACCOUNT") ||
                upper.contains("CARD") || upper.contains("USSD") || upper.contains("POS")) {
                return TransactionType.CREDIT;
            }
            // Disbursements are debits from the merchant
            if (upper.contains("DISBURSEMENT") || upper.contains("PAYOUT") ||
                upper.contains("TRANSFER_OUT")) {
                return TransactionType.DEBIT;
            }
        }
        
        // Check CHANNEL column
        String channelStr = extractColumn(line, columnMap, "CHANNEL");
        if (channelStr != null) {
            String upper = channelStr.toUpperCase();
            if (upper.contains("ACCOUNT_TRANSFER") || upper.contains("CARD")) {
                return TransactionType.CREDIT;
            }
            if (upper.contains("BANK_TRANSFER_OUT")) {
                return TransactionType.DEBIT;
            }
        }
        
        return super.determineTransactionType(line, columnMap);
    }
    
    @Override
    protected String normalizeStatus(String status) {
        if (status == null) return "SUCCESS";
        
        String upper = status.toUpperCase().trim();
        
        // Monnify-specific statuses
        if (upper.matches(".*(PAID|SUCCESSFUL|COMPLETED|SETTLED|SUCCESS).*")) {
            return "SUCCESS";
        }
        if (upper.matches(".*(PENDING|PROCESSING|INITIATED|PARTIAL|OVERPAID|UNDERPAID).*")) {
            return "PENDING";
        }
        if (upper.matches(".*(FAILED|REVERSED|CANCELLED|EXPIRED|ABANDONED).*")) {
            return "FAILED";
        }
        
        return super.normalizeStatus(status);
    }
    
    @Override
    protected ParsedTransaction parseRow(String[] line, Map<String, Integer> columnMap, int rowNum) {
        ParsedTransaction parsed = super.parseRow(line, columnMap, rowNum);
        
        if (parsed != null) {
            // Add Moniepoint-specific data to raw data
            Map<String, Object> rawData = parsed.getRawData();
            
            String transactionHash = extractColumn(line, columnMap, "TRANSACTION_HASH");
            String product = extractColumn(line, columnMap, "PRODUCT");
            String terminalId = extractColumn(line, columnMap, "TERMINAL_ID");
            String rrn = extractColumn(line, columnMap, "RRN");
            String cardType = extractColumn(line, columnMap, "CARD_TYPE");
            String maskedPan = extractColumn(line, columnMap, "MASKED_PAN");
            
            if (transactionHash != null) {
                rawData.put("transactionHash", transactionHash);
            }
            if (product != null) {
                rawData.put("product", product);
            }
            if (terminalId != null) {
                rawData.put("terminalId", terminalId);
            }
            if (rrn != null) {
                rawData.put("rrn", rrn);
            }
            if (cardType != null) {
                rawData.put("cardType", cardType);
            }
            if (maskedPan != null) {
                rawData.put("maskedPan", maskedPan);
            }
        }
        
        return parsed;
    }
}
