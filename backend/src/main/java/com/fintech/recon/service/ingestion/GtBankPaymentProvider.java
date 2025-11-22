package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class GtBankPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderName() {
        return "GTBank";
    }

    @Override
    public List<Transaction> fetchTransactions(LocalDateTime start, LocalDateTime end) {
        // Mock implementation for now
        return Collections.emptyList();
    }

    @Override
    public Transaction verifyTransaction(String reference) {
        // Mock implementation
        return null;
    }

    @Override
    public boolean initiateRefund(String reference, BigDecimal amount) {
        log.info("Initiating refund for reference: {} amount: {} via GTBank API", reference, amount);
        // Simulate API call latency
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate success
        log.info("Refund successful for reference: {}", reference);
        return true;
    }
}
