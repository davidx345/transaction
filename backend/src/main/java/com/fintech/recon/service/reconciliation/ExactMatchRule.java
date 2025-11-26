package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Exact Match Rule
 * Matches transactions with exact reference AND exact amount.
 * This is the highest confidence match.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExactMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    @Override
    public String getRuleName() {
        return "ExactMatch";
    }

    @Override
    public int getWeight() {
        // Exact match = reference points + amount points
        return config.getWeights().getExactReferenceMatch() + 
               config.getWeights().getExactAmountMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        String reference = transaction.getNormalizedReference();
        if (reference == null || reference.isEmpty()) {
            return false;
        }

        // Look for exact match in ledger
        Optional<Transaction> exactMatch = transactionRepository
                .findByNormalizedReferenceAndSource(reference, "ledger");

        if (exactMatch.isPresent()) {
            Transaction matched = exactMatch.get();
            
            // Verify amount also matches exactly
            if (matched.getAmount() != null && 
                transaction.getAmount() != null &&
                matched.getAmount().compareTo(transaction.getAmount()) == 0) {
                
                log.info("Exact match found: {} -> {} (amount: {})", 
                        reference, matched.getId(), matched.getAmount());
                
                context.put("matchedTransaction", matched);
                context.put("exactMatchFound", true);
                return true;
            }
        }

        return false;
    }
}
