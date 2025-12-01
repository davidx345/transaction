package com.fintech.recon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.domain.WebhookLog;
import com.fintech.recon.infrastructure.TransactionRepository;
import com.fintech.recon.infrastructure.WebhookLogRepository;
import com.fintech.recon.service.provider.PaymentProviderRegistry;
import com.fintech.recon.service.provider.WebhookHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookLogRepository webhookLogRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<WebhookLog>> getWebhooks(@RequestParam(required = false) String status) {
        List<WebhookLog> webhookLogs;
        
        if (status != null && !status.isEmpty()) {
            webhookLogs = webhookLogRepository.findByStatus(status);
        } else {
            webhookLogs = webhookLogRepository.findAll();
        }
        
        return ResponseEntity.ok(webhookLogs);
    }
    
    /**
     * Get list of supported providers
     */
    @GetMapping("/providers")
    public ResponseEntity<List<PaymentProviderRegistry.ProviderInfo>> getSupportedProviders() {
        return ResponseEntity.ok(providerRegistry.getProviderInfoList());
    }

    @PostMapping("/{provider}")
    public ResponseEntity<String> receiveWebhook(
            @PathVariable String provider,
            @RequestBody String rawPayload,
            HttpServletRequest request) {
        
        log.info("Received webhook from {}", provider);
        
        // Get handler for this provider
        Optional<WebhookHandler> handlerOpt = providerRegistry.getHandler(provider);
        
        if (handlerOpt.isEmpty()) {
            log.warn("Unknown provider: {}", provider);
            // Still log it for analysis
            return handleUnknownProvider(provider, rawPayload);
        }
        
        WebhookHandler handler = handlerOpt.get();
        
        try {
            // Parse payload
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            
            // Extract headers
            Map<String, String> headers = extractHeaders(request);
            
            // Get signature from appropriate header
            String signature = headers.get(handler.getSignatureHeaderName().toLowerCase());
            
            // Verify signature (log warning but don't reject in dev mode)
            boolean signatureValid = handler.verifySignature(rawPayload, signature, headers);
            if (!signatureValid) {
                log.warn("Invalid signature for {} webhook, proceeding anyway", provider);
            }
            
            // Extract reference and event type using handler
            String reference = handler.extractReference(payload);
            String eventType = handler.extractEventType(payload);
            boolean isSuccess = handler.isSuccessEvent(eventType, payload);
            
            // Create webhook log
            WebhookLog webhookLog = WebhookLog.builder()
                    .provider(provider)
                    .transactionRef(reference)
                    .eventType(eventType)
                    .receivedAt(LocalDateTime.now())
                    .status(signatureValid ? "received" : "signature_warning")
                    .payload(payload)
                    .build();
            
            webhookLogRepository.save(webhookLog);
            
            // Parse and save transaction if it's a transaction event
            if (reference != null && isTransactionEvent(eventType)) {
                try {
                    Transaction transaction = handler.parsePayload(payload);
                    // Store webhook log ID in rawData
                    if (transaction.getRawData() != null) {
                        transaction.getRawData().put("webhookLogId", webhookLog.getId().toString());
                    }
                    
                    // Check if transaction already exists using normalized reference
                    String normalizedRef = reference.toUpperCase().replaceAll("[^A-Z0-9]", "_");
                    Optional<Transaction> existing = transactionRepository
                            .findByNormalizedReferenceAndSource(normalizedRef, provider);
                    
                    if (existing.isPresent()) {
                        // Update existing transaction
                        Transaction existingTx = existing.get();
                        existingTx.setStatus(transaction.getStatus());
                        existingTx.setIngestedAt(LocalDateTime.now());
                        transactionRepository.save(existingTx);
                        log.info("Updated existing transaction: {}", reference);
                    } else {
                        // Save new transaction
                        transactionRepository.save(transaction);
                        log.info("Saved new transaction from webhook: {}", reference);
                    }
                } catch (Exception e) {
                    log.error("Error parsing transaction from webhook", e);
                }
            }
            
            return ResponseEntity.ok("Webhook received");
            
        } catch (Exception e) {
            log.error("Error processing webhook from {}", provider, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
    
    private ResponseEntity<String> handleUnknownProvider(String provider, String rawPayload) {
        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            
            // Try to extract reference from common locations
            String reference = extractReferenceFromPayload(payload);
            String eventType = (String) payload.get("event");
            
            WebhookLog webhookLog = WebhookLog.builder()
                    .provider(provider)
                    .transactionRef(reference)
                    .eventType(eventType)
                    .receivedAt(LocalDateTime.now())
                    .status("unknown_provider")
                    .payload(payload)
                    .build();
            
            webhookLogRepository.save(webhookLog);
            
            return ResponseEntity.ok("Webhook received (unknown provider)");
        } catch (Exception e) {
            log.error("Error handling unknown provider webhook", e);
            return ResponseEntity.ok("Webhook received");
        }
    }
    
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name.toLowerCase(), request.getHeader(name));
        }
        return headers;
    }
    
    @SuppressWarnings("unchecked")
    private String extractReferenceFromPayload(Map<String, Object> payload) {
        // Try data.reference
        if (payload.containsKey("data") && payload.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data.containsKey("reference")) {
                return data.get("reference").toString();
            }
        }
        // Try top-level reference fields
        if (payload.containsKey("reference")) {
            return payload.get("reference").toString();
        }
        if (payload.containsKey("paymentReference")) {
            return payload.get("paymentReference").toString();
        }
        if (payload.containsKey("transactionReference")) {
            return payload.get("transactionReference").toString();
        }
        return null;
    }
    
    private boolean isTransactionEvent(String eventType) {
        if (eventType == null) return true; // Assume it's a transaction if no event type
        String lower = eventType.toLowerCase();
        return lower.contains("success") || lower.contains("fail") || 
               lower.contains("paid") || lower.contains("complete") ||
               lower.contains("transfer") || lower.contains("charge") ||
               lower.contains("collection") || lower.contains("payout") ||
               lower.contains("deposit") || lower.contains("withdraw");
    }
}
