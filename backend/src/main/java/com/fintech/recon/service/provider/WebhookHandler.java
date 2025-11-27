package com.fintech.recon.service.provider;

import com.fintech.recon.domain.Transaction;
import com.fintech.recon.domain.WebhookLog;

import java.util.Map;

/**
 * Base interface for all payment provider webhook handlers
 */
public interface WebhookHandler {
    
    /**
     * Get the provider name (lowercase identifier)
     */
    String getProviderName();
    
    /**
     * Get the display name for the provider
     */
    String getDisplayName();
    
    /**
     * Verify webhook signature
     * @param payload Raw payload string
     * @param signature Signature from headers
     * @param headers All headers for additional context
     * @return true if signature is valid
     */
    boolean verifySignature(String payload, String signature, Map<String, String> headers);
    
    /**
     * Parse webhook payload to transaction
     * @param payload Parsed JSON payload
     * @return Transaction domain object
     */
    Transaction parsePayload(Map<String, Object> payload);
    
    /**
     * Extract reference from payload
     */
    String extractReference(Map<String, Object> payload);
    
    /**
     * Extract event type from payload
     */
    String extractEventType(Map<String, Object> payload);
    
    /**
     * Determine if this is a successful transaction event
     */
    boolean isSuccessEvent(String eventType, Map<String, Object> payload);
    
    /**
     * Get the signature header name for this provider
     */
    String getSignatureHeaderName();
}
