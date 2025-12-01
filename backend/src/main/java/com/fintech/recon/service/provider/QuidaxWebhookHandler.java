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
 * Webhook handler for Quidax (Cryptocurrency Exchange)
 * Signature: HMAC-SHA256 with timestamp.payload format
 * Header: quidax-signature
 */
@Component
@Slf4j
public class QuidaxWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "quidax";
    private static final String DISPLAY_NAME = "Quidax";
    private static final String SIGNATURE_HEADER = "quidax-signature";
    
    private static final Set<String> SUCCESS_EVENTS = Set.of(
            "deposit.successful", "withdraw.successful", "trade.completed"
    );
    
    @Value("${webhook.quidax-secret-key:sk_test_quidax_secret}")
    private String secretKey;
    
    private final ObjectMapper objectMapper;
    
    public QuidaxWebhookHandler(ObjectMapper objectMapper) {
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
            log.warn("Missing Quidax webhook signature");
            return false;
        }
        
        try {
            String timestamp = headers.get("quidax-timestamp");
            String signedPayload = timestamp + "." + payload;
            
            String expectedSignature = computeHmacSha256(signedPayload, secretKey);
            boolean valid = secureCompare(signature, expectedSignature);
            
            if (!valid) {
                log.warn("Invalid Quidax webhook signature");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Quidax signature", e);
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
        rawData.put("txid", getString(data, "txid"));
        rawData.put("confirmations", getString(data, "confirmations"));
        rawData.put("network", getString(data, "network"));
        
        String feeStr = getString(data, "fee");
        if (feeStr != null) {
            rawData.put("fee", new BigDecimal(feeStr));
        }
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        
        String amountStr = getString(data, "amount");
        transaction.setAmount(amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO);
        
        transaction.setCurrency(getString(data, "currency") != null ? getString(data, "currency") : "NGN");
        transaction.setStatus(mapStatus(getString(data, "status"), isSuccess));
        transaction.setTimestamp(parseDateTime(getString(data, "created_at")));
        transaction.setIngestedAt(LocalDateTime.now());
        transaction.setRawData(rawData);
        
        return transaction;
    }
    
    @Override
    public String extractReference(Map<String, Object> payload) {
        Map<String, Object> data = getDataObject(payload);
        String reference = getString(data, "reference");
        if (reference == null) {
            reference = getString(data, "id");
        }
        if (reference == null) {
            reference = getString(data, "txid");
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
