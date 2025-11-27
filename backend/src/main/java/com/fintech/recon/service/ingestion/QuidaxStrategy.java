package com.fintech.recon.service.ingestion;

import com.fintech.recon.dto.CsvParseResult.ParsedTransaction;
import com.fintech.recon.dto.CsvParseResult.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CSV parser strategy for Quidax transaction exports.
 * 
 * Quidax is a cryptocurrency exchange - export format includes:
 * - REFERENCE/TXID, AMOUNT, CURRENCY, TYPE, STATUS, CREATED_AT, etc.
 * - Date format: yyyy-MM-dd'T'HH:mm:ss
 * - Reference prefix: QDX-
 * 
 * Supported transaction types:
 * - Crypto deposits
 * - Crypto withdrawals
 * - Fiat deposits/withdrawals (NGN)
 * - Trade executions (buy/sell)
 * - P2P transactions
 */
@Component
@Slf4j
public class QuidaxStrategy extends AbstractBankStrategy {
    
    public QuidaxStrategy(ReferenceExtractor referenceExtractor) {
        super(referenceExtractor);
    }
    
    @Override
    protected BankFormat getBankFormat() {
        return BankFormat.QUIDAX;
    }
    
    @Override
    public String getBankName() {
        return "Quidax";
    }
    
    @Override
    protected Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase().trim().replace(" ", "_").replace("-", "_");
            
            // Reference columns - Quidax uses TXID for blockchain transactions
            if (header.equals("REFERENCE") || header.equals("TXID") || 
                header.equals("TRANSACTION_ID") || header.equals("ID") ||
                header.equals("QUIDAX_REF") || header.equals("BLOCKCHAIN_HASH")) {
                if (!columnMap.containsKey("REFERENCE")) {
                    columnMap.put("REFERENCE", i);
                }
            }
            
            // Date columns
            if (header.equals("CREATED_AT") || header.equals("CREATED") ||
                header.equals("TIMESTAMP") || header.equals("DATE")) {
                if (!columnMap.containsKey("DATE")) {
                    columnMap.put("DATE", i);
                }
            }
            if (header.equals("CONFIRMED_AT") || header.equals("COMPLETED_AT")) {
                columnMap.put("CONFIRMED_DATE", i);
            }
            
            // Amount columns - crypto amounts can be in QUANTITY
            if (header.equals("AMOUNT") || header.equals("VALUE") || 
                header.equals("TOTAL")) {
                columnMap.put("AMOUNT", i);
            }
            if (header.equals("QUANTITY") || header.equals("CRYPTO_AMOUNT")) {
                columnMap.put("QUANTITY", i);
            }
            if (header.equals("FEE") || header.equals("NETWORK_FEE") || 
                header.equals("TRANSACTION_FEE")) {
                columnMap.put("FEE", i);
            }
            
            // Currency / Crypto asset
            if (header.equals("CURRENCY") || header.equals("ASSET") ||
                header.equals("COIN") || header.equals("CRYPTO")) {
                columnMap.put("CURRENCY", i);
            }
            
            // Status
            if (header.equals("STATUS") || header.equals("STATE") ||
                header.equals("TRANSACTION_STATUS")) {
                columnMap.put("STATUS", i);
            }
            
            // Transaction type (deposit, withdrawal, trade, etc.)
            if (header.equals("TYPE") || header.equals("TRANSACTION_TYPE") ||
                header.equals("CATEGORY") || header.equals("ACTION")) {
                columnMap.put("TYPE", i);
            }
            
            // Confirmations (blockchain specific)
            if (header.equals("CONFIRMATIONS") || header.equals("CONFIRMS")) {
                columnMap.put("CONFIRMATIONS", i);
            }
            
            // Network (blockchain network)
            if (header.equals("NETWORK") || header.equals("BLOCKCHAIN") ||
                header.equals("CHAIN")) {
                columnMap.put("NETWORK", i);
            }
            
            // Wallet address
            if (header.equals("ADDRESS") || header.equals("WALLET_ADDRESS") ||
                header.equals("DESTINATION_ADDRESS") || header.equals("FROM_ADDRESS")) {
                if (!columnMap.containsKey("ADDRESS")) {
                    columnMap.put("ADDRESS", i);
                }
            }
            
            // Customer/User info
            if (header.contains("USER") || header.equals("EMAIL") || 
                header.equals("USER_ID") || header.equals("CUSTOMER")) {
                if (!columnMap.containsKey("CUSTOMER")) {
                    columnMap.put("CUSTOMER", i);
                }
            }
            
            // Trade-specific columns
            if (header.equals("PAIR") || header.equals("TRADING_PAIR") ||
                header.equals("MARKET")) {
                columnMap.put("PAIR", i);
            }
            if (header.equals("SIDE") || header.equals("ORDER_SIDE")) {
                columnMap.put("SIDE", i);
            }
            if (header.equals("PRICE") || header.equals("RATE") ||
                header.equals("EXCHANGE_RATE")) {
                columnMap.put("PRICE", i);
            }
        }
        
        // Use confirmed date as primary if available
        if (columnMap.containsKey("CONFIRMED_DATE") && !columnMap.containsKey("DATE")) {
            columnMap.put("DATE", columnMap.get("CONFIRMED_DATE"));
        }
        
        // Use quantity as amount fallback for crypto
        if (!columnMap.containsKey("AMOUNT") && columnMap.containsKey("QUANTITY")) {
            columnMap.put("AMOUNT", columnMap.get("QUANTITY"));
        }
        
        log.debug("Quidax column mapping: {}", columnMap);
        return columnMap;
    }
    
    @Override
    protected TransactionType determineTransactionType(String[] line, Map<String, Integer> columnMap) {
        // Check TYPE column first for crypto-specific types
        String typeStr = extractColumn(line, columnMap, "TYPE");
        if (typeStr != null) {
            String upper = typeStr.toUpperCase();
            if (upper.contains("DEPOSIT") || upper.contains("BUY") || upper.contains("RECEIVE")) {
                return TransactionType.CREDIT;
            }
            if (upper.contains("WITHDRAW") || upper.contains("SELL") || upper.contains("SEND")) {
                return TransactionType.DEBIT;
            }
        }
        
        // Check SIDE for trades
        String sideStr = extractColumn(line, columnMap, "SIDE");
        if (sideStr != null) {
            String upper = sideStr.toUpperCase();
            if (upper.contains("BUY")) {
                return TransactionType.CREDIT;
            }
            if (upper.contains("SELL")) {
                return TransactionType.DEBIT;
            }
        }
        
        return super.determineTransactionType(line, columnMap);
    }
    
    @Override
    protected String normalizeStatus(String status) {
        if (status == null) return "SUCCESS";
        
        String upper = status.toUpperCase().trim();
        
        // Quidax-specific statuses
        if (upper.matches(".*(CONFIRMED|COMPLETE|DONE|SUCCESSFUL|SETTLED).*")) {
            return "SUCCESS";
        }
        if (upper.matches(".*(PENDING|PROCESSING|CONFIRMING|UNCONFIRMED).*")) {
            return "PENDING";
        }
        if (upper.matches(".*(FAILED|REJECTED|CANCELLED|EXPIRED|INVALID).*")) {
            return "FAILED";
        }
        
        return super.normalizeStatus(status);
    }
}
