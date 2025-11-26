package com.fintech.recon.service.ingestion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Enum representing supported Nigerian bank CSV formats.
 * Contains metadata about each bank's statement structure for auto-detection and parsing.
 */
@Getter
@RequiredArgsConstructor
public enum BankFormat {
    
    GTBANK(
        "GTBank",
        List.of("PAYMENT_REF", "TRANS REF", "TRANSACTION REF"),
        List.of("AMOUNT", "DEBIT", "CREDIT"),
        List.of("SETTLEMENT_DATE", "TXN DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "GTB-",
        List.of("GTB", "GUARANTY", "GTBANK")
    ),
    
    ACCESS_BANK(
        "AccessBank",
        List.of("REFERENCE", "REF NO", "TRANS REF"),
        List.of("AMOUNT", "DEBIT AMT", "CREDIT AMT"),
        List.of("POST DATE", "VALUE DATE", "TRANS DATE"),
        "dd-MMM-yyyy", // 22-Nov-2024
        "ACC-",
        List.of("ACCESS", "ACC-", "DIAMOND") // Diamond Bank merged with Access
    ),
    
    ZENITH_BANK(
        "ZenithBank",
        List.of("TRANS ID", "REFERENCE", "REF"),
        List.of("DR AMOUNT", "CR AMOUNT", "AMOUNT"),
        List.of("TRANS DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "ZEN-",
        List.of("ZENITH", "ZEN-", "ZENB")
    ),
    
    FIRST_BANK(
        "FirstBank",
        List.of("REFERENCE", "TRANS REF", "TRANSACTION REFERENCE"),
        List.of("DEBIT", "CREDIT", "AMOUNT"),
        List.of("TRANSACTION DATE", "VALUE DATE", "POST DATE"),
        "yyyy-MM-dd", // ISO format
        "FBN-",
        List.of("FIRST BANK", "FBN", "FIRSTBANK")
    ),
    
    UBA(
        "UBA",
        List.of("TRAN REF", "REFERENCE", "REF NO"),
        List.of("DEBIT", "CREDIT", "DR", "CR"),
        List.of("TRAN DATE", "TRANS DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "UBA-",
        List.of("UBA", "UNITED BANK")
    ),
    
    STANBIC_IBTC(
        "StanbicIBTC",
        List.of("TRANS REF", "REFERENCE"),
        List.of("DEBIT", "CREDIT"),
        List.of("DATE", "VALUE DATE", "POST DATE"),
        "dd/MM/yyyy",
        "STB-",
        List.of("STANBIC", "IBTC", "STB-")
    ),
    
    ECOBANK(
        "Ecobank",
        List.of("REFERENCE", "TRANS REF"),
        List.of("DEBIT", "CREDIT", "AMOUNT"),
        List.of("VALUE DATE", "TRANS DATE"),
        "dd-MM-yyyy",
        "ECO-",
        List.of("ECOBANK", "ECO-")
    ),
    
    FIDELITY_BANK(
        "FidelityBank",
        List.of("REF", "REFERENCE", "TRANS REF"),
        List.of("DR", "CR", "DEBIT", "CREDIT"),
        List.of("TRANS DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "FID-",
        List.of("FIDELITY", "FID-")
    ),
    
    UNION_BANK(
        "UnionBank",
        List.of("TRAN ID", "REFERENCE"),
        List.of("DEBIT", "CREDIT"),
        List.of("TRANS DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "UNB-",
        List.of("UNION BANK", "UNB-")
    ),
    
    PAYSTACK(
        "Paystack",
        List.of("REFERENCE", "PAYMENT_REFERENCE", "ID"),
        List.of("AMOUNT", "NET_AMOUNT"),
        List.of("PAID_AT", "CREATED_AT", "DATE"),
        "yyyy-MM-dd'T'HH:mm:ss",
        "PSK_",
        List.of("PAYSTACK", "PSK_", "PS-")
    ),
    
    FLUTTERWAVE(
        "Flutterwave",
        List.of("TX_REF", "FLUTTERWAVEREF", "REFERENCE"),
        List.of("AMOUNT", "CHARGED_AMOUNT"),
        List.of("CREATED_AT", "DATE"),
        "yyyy-MM-dd HH:mm:ss",
        "FLW-",
        List.of("FLUTTERWAVE", "FLW-", "RAVE")
    ),
    
    GENERIC(
        "Generic",
        List.of("REFERENCE", "REF", "ID"),
        List.of("AMOUNT", "DEBIT", "CREDIT"),
        List.of("DATE", "TRANS DATE", "VALUE DATE"),
        "dd/MM/yyyy",
        "",
        List.of()
    );
    
    private final String displayName;
    private final List<String> referenceHeaders;
    private final List<String> amountHeaders;
    private final List<String> dateHeaders;
    private final String dateFormat;
    private final String referencePrefix;
    private final List<String> identifiers;
    
    /**
     * Get DateTimeFormatter for this bank's date format
     */
    public DateTimeFormatter getDateFormatter() {
        return DateTimeFormatter.ofPattern(dateFormat);
    }
    
    /**
     * Detect bank format from CSV header row
     */
    public static Optional<BankFormat> detectFromHeader(String[] headers) {
        String headerLine = String.join(" ", headers).toUpperCase();
        
        // Check each bank's identifiers
        for (BankFormat format : values()) {
            if (format == GENERIC) continue;
            
            for (String identifier : format.identifiers) {
                if (headerLine.contains(identifier.toUpperCase())) {
                    return Optional.of(format);
                }
            }
        }
        
        // Try to detect from reference column patterns
        for (BankFormat format : values()) {
            if (format == GENERIC) continue;
            
            for (String refHeader : format.referenceHeaders) {
                if (headerLine.contains(refHeader.toUpperCase())) {
                    return Optional.of(format);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Detect bank format from transaction reference pattern
     */
    public static Optional<BankFormat> detectFromReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return Optional.empty();
        }
        
        String upperRef = reference.toUpperCase();
        
        for (BankFormat format : values()) {
            if (format == GENERIC) continue;
            
            if (!format.referencePrefix.isEmpty() && 
                upperRef.startsWith(format.referencePrefix.toUpperCase())) {
                return Optional.of(format);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get bank format by name (case-insensitive)
     */
    public static Optional<BankFormat> fromName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
            .filter(f -> f.displayName.equalsIgnoreCase(name) || 
                        f.name().equalsIgnoreCase(name))
            .findFirst();
    }
}
