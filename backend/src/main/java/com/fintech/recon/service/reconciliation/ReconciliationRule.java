package com.fintech.recon.service.reconciliation;

import com.fintech.recon.domain.Transaction;
import java.util.Map;

public interface ReconciliationRule {
    String getRuleName();
    int getWeight();
    boolean evaluate(Transaction transaction, Map<String, Object> context);
}
