package com.fintech.recon.service.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.recon.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Webhook handler for Kora (Korapay)
 * Signature: HMAC-SHA256 on data object only
 * Header: x-korapay-signature
 */
@Component
@Slf4j
public class KoraWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "kora";
    private static final String DISPLAY_NAME = "Kora (Korapay)";
    private static final String SIGNATURE_HEADER = "x-korapay-signature";
    
    private static final Set<String> SUCCESS_EVENTS = Set.of(
            "transfer.success", "charge.success", "refund.success"
    );
    
    private static final Set<String> FAILURE_EVENTS = Set.of(
            "transfer.failed", "charge.failed", "refund.failed"
    );
    
    @Value("${webhook.kora.secret-key:sk_test_kora_secret}")
    private String secretKey;
    
    private final ObjectMapper objectMapper;
    
    public KoraWebhookHandler(ObjectMapper objectMapper) {
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
            log.warn("Missing Kora webhook signature");
            return false;
        }
        
        try {
            // Kora signs only the data object
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            Object dataObject = payloadMap.get("data");
            String dataJson = objectMapper.writeValueAsString(dataObject);
            
            String expectedSignature = computeHmacSha256(dataJson, secretKey);
            boolean valid = secureCompare(signature, expectedSignature);
            
            if (!valid) {
                log.warn("Invalid Kora webhook signature");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Kora signature", e);
            return false;
        }
    }
    
    @Override
    public Transaction parsePayload(Map<String, Object> payload) {
        Map<String, Object> data = getDataObject(payload);
        String eventType = extractEventType(payload);
        boolean isSuccess = isSuccessEvent(eventType, payload);
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        transaction.setAmount(getBigDecimal(data, "amount"));
        transaction.setFee(getBigDecimal(data, "fee"));
        transaction.setCurrency(getString(data, "currency"));
        transaction.setStatus(mapStatus(getString(data, "status"), isSuccess));
        transaction.setDescription(getString(data, "description"));
        transaction.setTimestamp(parseDateTime(getString(data, "completed_at")));
        transaction.setCreatedAt(LocalDateTime.now());
        
        // Set transaction type based on event
        if (eventType != null) {
            if (eventType.startsWith("charge")) {
                transaction.setType(Transaction.TransactionType.CREDIT);
            } else if (eventType.startsWith("transfer")) {
                transaction.setType(Transaction.TransactionType.DEBIT);
            } else if (eventType.startsWith("refund")) {
                transaction.setType(Transaction.TransactionType.REFUND);
            }
        }
        
        return transaction;
    }
    
    @Override
    public String extractReference(Map<String, Object> payload) {
        Map<String, Object> data = getDataObject(payload);
        String reference = getString(data, "reference");
        if (reference == null) {
            reference = getString(data, "payment_reference");
        }
        if (reference == null) {
            reference = getString(data, "payout_reference");
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
        // Kora references typically start with KPY_ or similar
        return reference.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}
