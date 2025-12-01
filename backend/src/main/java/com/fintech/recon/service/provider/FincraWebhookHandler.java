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
 * Webhook handler for Fincra (Cross-Border Payments)
 * Signature: Encryption key verification
 * Header: x-fincra-signature
 */
@Component
@Slf4j
public class FincraWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "fincra";
    private static final String DISPLAY_NAME = "Fincra";
    private static final String SIGNATURE_HEADER = "x-fincra-signature";
    
    private static final Set<String> SUCCESS_EVENTS = Set.of(
            "collection.successful", "payout.successful", "conversion.successful"
    );
    
    @Value("${webhook.fincra-encryption-key:enc_test_fincra_key}")
    private String encryptionKey;
    
    private final ObjectMapper objectMapper;
    
    public FincraWebhookHandler(ObjectMapper objectMapper) {
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
            log.warn("Missing Fincra webhook signature");
            return false;
        }
        
        try {
            String expectedSignature = computeHmacSha256(payload, encryptionKey);
            boolean valid = secureCompare(signature, expectedSignature);
            
            if (!valid) {
                log.warn("Invalid Fincra webhook signature");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Fincra signature", e);
            return false;
        }
    }
    
    @Override
    public Transaction parsePayload(Map<String, Object> payload) {
        Map<String, Object> data = getDataObject(payload);
        String eventType = extractEventType(payload);
        boolean isSuccess = isSuccessEvent(eventType, payload);
        
        Map<String, Object> rawData = new HashMap<>(data);
        rawData.put("event_type", eventType);
        rawData.put("sessionId", getString(data, "sessionId"));
        rawData.put("sourceCurrency", getString(data, "sourceCurrency"));
        rawData.put("destinationCurrency", getString(data, "destinationCurrency"));
        rawData.put("sourceAmount", getBigDecimal(data, "sourceAmount"));
        rawData.put("fee", getBigDecimal(data, "fee"));
        
        BigDecimal sourceAmount = getBigDecimal(data, "sourceAmount");
        BigDecimal amountReceived = getBigDecimal(data, "amountReceived");
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        transaction.setAmount(amountReceived.compareTo(BigDecimal.ZERO) > 0 ? amountReceived : sourceAmount);
        
        String currency = getString(data, "destinationCurrency");
        if (currency == null) {
            currency = getString(data, "sourceCurrency");
        }
        transaction.setCurrency(currency != null ? currency : "NGN");
        
        transaction.setStatus(mapStatus(getString(data, "status"), isSuccess));
        transaction.setTimestamp(parseDateTime(getString(data, "createdAt")));
        transaction.setIngestedAt(LocalDateTime.now());
        transaction.setRawData(rawData);
        
        return transaction;
    }
    
    @Override
    public String extractReference(Map<String, Object> payload) {
        Map<String, Object> data = getDataObject(payload);
        String reference = getString(data, "reference");
        if (reference == null) {
            reference = getString(data, "customerReference");
        }
        if (reference == null) {
            reference = getString(data, "sessionId");
        }
        return reference;
    }
    
    @Override
    public String extractEventType(Map<String, Object> payload) {
        return getString(payload, "event");
    }
    
    @Override
    public boolean isSuccessEvent(String eventType, Map<String, Object> payload) {
        return eventType != null && SUCCESS_EVENTS.contains(eventType.toLowerCase());
    }
    
    private String normalizeReference(String reference) {
        if (reference == null) return null;
        return reference.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}
