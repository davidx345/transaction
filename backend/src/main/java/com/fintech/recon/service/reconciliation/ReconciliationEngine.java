package com.fintech.recon.service.reconciliation;

import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationEngine {

    private final ReconciliationRepository reconciliationRepository;
    private final List<ReconciliationRule> rules;

    public Reconciliation reconcile(Transaction transaction) {
        log.info("Reconciling transaction: {}", transaction.getNormalizedReference());

        // 1. Gather Context (e.g., find potential matches in Ledger/Bank)
        // This is a simplified view; in reality, we'd query by reference, amount, etc.
        Map<String, Object> context = new HashMap<>();
        // context.put("ledgerMatch", transactionRepository.findByReference(...));

        int totalScore = 0;
        List<Map<String, Object>> rulesFired = new ArrayList<>();

        // 2. Apply Rules
        for (ReconciliationRule rule : rules) {
            if (rule.evaluate(transaction, context)) {
                totalScore += rule.getWeight();
                Map<String, Object> ruleInfo = new HashMap<>();
                ruleInfo.put("rule", rule.getRuleName());
                ruleInfo.put("weight", rule.getWeight());
                rulesFired.add(ruleInfo);
            }
        }

        // 3. Determine State based on Score
        String state = determineState(totalScore);

        // 4. Create Result
        Reconciliation reconciliation = Reconciliation.builder()
                .transactionRef(transaction.getNormalizedReference())
                .runId(UUID.randomUUID().toString())
                .confidenceScore(totalScore)
                .rulesFired(rulesFired)
                .state(state)
                .build();
        
        // 5. Persist Result
        return reconciliationRepository.save(reconciliation);
    }

    private String determineState(int score) {
        if (score >= 90) return "HIGH_CONFIDENCE";
        if (score >= 70) return "MEDIUM_CONFIDENCE";
        if (score >= 40) return "LOW_CONFIDENCE";
        return "IGNORE";
    }
}
