package com.fintech.recon.web;

import com.fintech.recon.domain.WebhookLog;
import com.fintech.recon.infrastructure.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookLogRepository webhookLogRepository;

    @PostMapping("/{provider}")
    public ResponseEntity<String> receiveWebhook(@PathVariable String provider,
                                                 @RequestBody Map<String, Object> payload) {
        log.info("Received webhook from {}: {}", provider, payload);

        // Extract reference (simplified logic, assumes 'data.reference' exists)
        String reference = null;
        if (payload.containsKey("data") && payload.get("data") instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) payload.get("data");
            if (data.containsKey("reference")) {
                reference = data.get("reference").toString();
            }
        }

        WebhookLog webhookLog = WebhookLog.builder()
                .provider(provider)
                .transactionRef(reference)
                .eventType((String) payload.get("event"))
                .receivedAt(LocalDateTime.now())
                .status("received")
                .payload(payload)
                .build();

        webhookLogRepository.save(webhookLog);

        return ResponseEntity.ok("Webhook received");
    }
}
