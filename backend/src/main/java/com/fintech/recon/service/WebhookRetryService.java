package com.fintech.recon.service;

import com.fintech.recon.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryService {

    private final RabbitTemplate rabbitTemplate;

    public void retryWebhook(String transactionRef, int attempt) {
        long delay = calculateBackoff(attempt);
        log.info("Scheduling retry for webhook {} (Attempt {}), delay: {}ms", transactionRef, attempt, delay);

        // In a real implementation, we would use a Delayed Exchange or a Scheduled Task
        // For MVP, we'll simulate by sending to DLQ if max attempts reached
        
        if (attempt > 5) {
            log.warn("Max retry attempts reached for {}. Sending to DLQ.", transactionRef);
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_WEBHOOK_DLX, "webhook.failed", transactionRef);
        } else {
            // Logic to re-fetch status from provider and re-process
            // ...
        }
    }

    private long calculateBackoff(int attempt) {
        // Exponential backoff: 2^attempt * 1000ms
        return (long) Math.pow(2, attempt) * 1000;
    }
}
