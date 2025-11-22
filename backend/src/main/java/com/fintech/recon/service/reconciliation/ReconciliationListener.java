package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.RabbitMqConfig;
import com.fintech.recon.domain.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationListener {

    private final ReconciliationEngine reconciliationEngine;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_RECONCILIATION)
    public void handleTransactionIngested(Transaction transaction) {
        log.info("Received transaction for reconciliation: {}", transaction.getNormalizedReference());
        try {
            reconciliationEngine.reconcile(transaction);
        } catch (Exception e) {
            log.error("Error reconciling transaction: {}", transaction.getId(), e);
            // In a real system, we might reject and send to DLQ here
        }
    }
}
