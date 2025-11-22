package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentProvider {
    String getProviderName();
    List<Transaction> fetchTransactions(LocalDateTime start, LocalDateTime end);
    Transaction verifyTransaction(String reference);
    boolean initiateRefund(String reference, java.math.BigDecimal amount);
}
