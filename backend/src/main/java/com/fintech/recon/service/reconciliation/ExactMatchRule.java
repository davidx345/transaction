package com.fintech.recon.service.reconciliation;

import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExactMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;

    @Override
    public String getRuleName() {
        return "ExactMatch";
    }

    @Override
    public int getWeight() {
        return 100; // High confidence if exact match found
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        // Look for a matching transaction in the 'ledger' source
        // Criteria: Same Normalized Reference AND Same Amount
        
        // In a real scenario, we might want to optimize this query or use the context map
        // to avoid hitting the DB for every rule if data is already fetched.
        
        // For MVP: Query DB directly
        Optional<Transaction> match = transactionRepository.findAll().stream() // TODO: Replace with findByNormalizedReferenceAndSource
                .filter(t -> "ledger".equalsIgnoreCase(t.getSource()))
                .filter(t -> t.getNormalizedReference().equals(transaction.getNormalizedReference()))
                .filter(t -> t.getAmount().compareTo(transaction.getAmount()) == 0)
                .findFirst();

        if (match.isPresent()) {
            context.put("matchedTransaction", match.get());
            return true;
        }
        
        return false;
    }
}
