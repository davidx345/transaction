package com.fintech.recon.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.recon.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Webhook handler for Moniepoint/Monnify
 * Signature: SHA512 hash of transactionHash field
 * Header: monnify-signature
 */
@Component
@Slf4j
public class MoniepointWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "moniepoint";
    private static final String DISPLAY_NAME = "Moniepoint/Monnify";
    private static final String SIGNATURE_HEADER = "monnify-signature";
    
    private static final Set<String> SUCCESS_STATUSES = Set.of(
            "PAID", "SUCCESSFUL", "COMPLETED"
    );
    
    @Value("${webhook.moniepoint-secret-key:sk_test_moniepoint_secret}")
    private String secretKey;
    
    private final ObjectMapper objectMapper;
    
    public MoniepointWebhookHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
    
    @Override
    public String getSignatureHeaderName() {
        return SIGNATURE_HEADER;
    }
    
    @Override
    public boolean verifySignature(String payload, String signature, Map<String, String> headers) {
        if (signature == null || signature.isEmpty()) {
            log.warn("Missing Moniepoint webhook signature");
            return false;
        }
        
        try {
            String expectedSignature = computeHmacSha512(payload, secretKey);
            boolean valid = secureCompare(signature, expectedSignature);
            
            if (!valid) {
                log.warn("Invalid Moniepoint webhook signature");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Moniepoint signature", e);
            return false;
        }
    }
    
    @Override
    public Transaction parsePayload(Map<String, Object> payload) {
        String paymentStatus = getString(payload, "paymentStatus");
        boolean isSuccess = paymentStatus != null && SUCCESS_STATUSES.contains(paymentStatus.toUpperCase());
        
        Map<String, Object> rawData = new HashMap<>(payload);
        rawData.put("transactionHash", getString(payload, "transactionHash"));
        rawData.put("paymentMethod", getString(payload, "paymentMethod"));
        rawData.put("paymentDescription", getString(payload, "paymentDescription"));
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        
        String amountStr = getString(payload, "amountPaid");
        if (amountStr != null) {
            transaction.setAmount(new BigDecimal(amountStr.replace(",", "")));
        } else {
            transaction.setAmount(BigDecimal.ZERO);
        }
        
        // Calculate fee from totalPayable - settlementAmount
        String totalStr = getString(payload, "totalPayable");
        String settlementStr = getString(payload, "settlementAmount");
        if (totalStr != null && settlementStr != null) {
            BigDecimal total = new BigDecimal(totalStr.replace(",", ""));
            BigDecimal settlement = new BigDecimal(settlementStr.replace(",", ""));
            rawData.put("fee", total.subtract(settlement));
        }
        
        transaction.setCurrency(getString(payload, "currency") != null ? getString(payload, "currency") : "NGN");
        transaction.setStatus(mapStatus(paymentStatus, isSuccess));
        transaction.setTimestamp(parseDateTime(getString(payload, "paidOn")));
        transaction.setIngestedAt(LocalDateTime.now());
        transaction.setRawData(rawData);
        
        return transaction;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String extractReference(Map<String, Object> payload) {
        String reference = getString(payload, "paymentReference");
        if (reference == null) {
            reference = getString(payload, "transactionReference");
        }
        if (reference == null) {
            Object product = payload.get("product");
            if (product instanceof Map) {
                reference = getString((Map<String, Object>) product, "reference");
            }
        }
        return reference;
    }
    
    @Override
    public String extractEventType(Map<String, Object> payload) {
        return getString(payload, "eventType");
    }
    
    @Override
    public boolean isSuccessEvent(String eventType, Map<String, Object> payload) {
        String paymentStatus = getString(payload, "paymentStatus");
        return paymentStatus != null && SUCCESS_STATUSES.contains(paymentStatus.toUpperCase());
    }
    
    private String normalizeReference(String reference) {
        if (reference == null) return null;
        return reference.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}
