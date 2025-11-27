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
 * Webhook handler for Quidax cryptocurrency exchange
 * Signature: HMAC-SHA256 with timestamp.payload format
 * Header: quidax-signature (format: t=timestamp,v1=signature)
 */
@Component
@Slf4j
public class QuidaxWebhookHandler extends AbstractWebhookHandler {
    
    private static final String PROVIDER_NAME = "quidax";
    private static final String DISPLAY_NAME = "Quidax";
    private static final String SIGNATURE_HEADER = "quidax-signature";
    
    private static final Set<String> SUCCESS_EVENTS = Set.of(
            "deposit.successful", "withdraw.successful", "order.done", 
            "swap.completed", "buy.transaction.successful", "sell.transaction.successful"
    );
    
    private static final Set<String> FAILURE_EVENTS = Set.of(
            "deposit.failed", "deposit.rejected", "withdraw.rejected", 
            "order.cancelled", "swap.failed", "swap.reversed",
            "buy.transaction.failed", "sell.transaction.failed"
    );
    
    @Value("${webhook.quidax.webhook-key:quidax_webhook_secret}")
    private String webhookKey;
    
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
            // Parse signature format: t=timestamp,v1=signature
            String[] parts = signature.split(",");
            String timestamp = null;
            String sig = null;
            
            for (String part : parts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    sig = part.substring(3);
                }
            }
            
            if (timestamp == null || sig == null) {
                log.warn("Invalid Quidax signature format");
                return false;
            }
            
            // Compute expected signature: HMAC-SHA256(timestamp.payload)
            String signPayload = timestamp + "." + payload;
            String expectedSignature = computeHmacSha256(signPayload, webhookKey);
            
            boolean valid = secureCompare(sig, expectedSignature);
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
        
        Transaction transaction = new Transaction();
        transaction.setSource(PROVIDER_NAME);
        transaction.setExternalReference(extractReference(payload));
        transaction.setNormalizedReference(normalizeReference(extractReference(payload)));
        
        // Quidax amounts are strings
        String amountStr = getString(data, "amount");
        transaction.setAmount(amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO);
        
        String feeStr = getString(data, "fee");
        transaction.setFee(feeStr != null ? new BigDecimal(feeStr) : BigDecimal.ZERO);
        
        transaction.setCurrency(getString(data, "currency"));
        transaction.setStatus(mapStatus(getString(data, "status"), isSuccess));
        transaction.setDescription(getString(data, "transaction_note"));
        transaction.setTimestamp(parseDateTime(getString(data, "created_at")));
        transaction.setCreatedAt(LocalDateTime.now());
        
        // Set transaction type based on event
        if (eventType != null) {
            String lowerEvent = eventType.toLowerCase();
            if (lowerEvent.contains("deposit") || lowerEvent.contains("buy")) {
                transaction.setType(Transaction.TransactionType.CREDIT);
            } else if (lowerEvent.contains("withdraw") || lowerEvent.contains("sell")) {
                transaction.setType(Transaction.TransactionType.DEBIT);
            } else if (lowerEvent.contains("swap")) {
                transaction.setType(Transaction.TransactionType.TRANSFER);
            }
        }
        
        // Store blockchain txid if available
        String txid = getString(data, "txid");
        if (txid != null) {
            transaction.setProviderTransactionId(txid);
        }
        
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
