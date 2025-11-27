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
 * Webhook handler for Fincra cross-border payments
 * Signature: Encryption key verification
 */
@Component
@Slf4j
public class FincraWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "fincra";
    private static final String DISPLAY_NAME = "Fincra";
    private static final String SIGNATURE_HEADER = "x-fincra-signature";
    
    private static final Set<String> SUCCESS_EVENTS = Set.of(
            "collection.successful", "payout.successful", "conversion.completed",
            "virtualaccount.created"
    );
    
    private static final Set<String> FAILURE_EVENTS = Set.of(
            "collection.failed", "payout.failed", "conversion.failed"
    );
    
    @Value("${webhook.fincra.encryption-key:fincra_encryption_key}")
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
            // For development, allow without signature
            return true;
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
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        
        // Fincra has source and destination amounts for FX
        BigDecimal sourceAmount = getBigDecimal(data, "sourceAmount");
        BigDecimal amountReceived = getBigDecimal(data, "amountReceived");
        transaction.setAmount(amountReceived.compareTo(BigDecimal.ZERO) > 0 ? amountReceived : sourceAmount);
        
        transaction.setFee(getBigDecimal(data, "fee"));
        
        // Use destination currency if available, otherwise source
        String currency = getString(data, "destinationCurrency");
        if (currency == null) {
            currency = getString(data, "sourceCurrency");
        }
        transaction.setCurrency(currency);
        
        transaction.setStatus(mapStatus(getString(data, "status"), isSuccess));
        transaction.setDescription(getString(data, "description"));
        transaction.setTimestamp(parseDateTime(getString(data, "createdAt")));
        transaction.setCreatedAt(LocalDateTime.now());
        
        // Store session ID
        String sessionId = getString(data, "sessionId");
        if (sessionId != null) {
            transaction.setProviderTransactionId(sessionId);
        }
        
        // Set transaction type based on event
        if (eventType != null) {
            String lowerEvent = eventType.toLowerCase();
            if (lowerEvent.contains("collection")) {
                transaction.setType(Transaction.TransactionType.CREDIT);
            } else if (lowerEvent.contains("payout")) {
                transaction.setType(Transaction.TransactionType.DEBIT);
            } else if (lowerEvent.contains("conversion")) {
                transaction.setType(Transaction.TransactionType.TRANSFER);
            }
        }
        
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
