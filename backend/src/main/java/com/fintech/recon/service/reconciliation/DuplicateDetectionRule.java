package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Duplicate Detection Rule
 * Detects potential duplicate transactions that might indicate:
 * - Double charges
 * - Duplicate refunds
 * - System re-processing errors
 * 
 * This rule applies a PENALTY (negative score) when duplicates are found
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    // Time window to check for duplicates (in minutes)
    private static final int DUPLICATE_WINDOW_MINUTES = 30;

    @Override
    public String getRuleName() {
        return "DuplicateDetection";
    }

    @Override
    public int getWeight() {
        // This returns a PENALTY (negative value)
        return config.getWeights().getDuplicatePenalty();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        if (transaction.getId() == null || transaction.getAmount() == null || transaction.getTimestamp() == null) {
            return false;
        }

        LocalDateTime startDate = transaction.getTimestamp().minusMinutes(DUPLICATE_WINDOW_MINUTES);
        LocalDateTime endDate = transaction.getTimestamp().plusMinutes(DUPLICATE_WINDOW_MINUTES);

        // Find potential duplicates: same source, same amount, within time window
        List<Transaction> potentialDuplicates = transactionRepository.findPotentialDuplicates(
                transaction.getSource(),
                transaction.getAmount(),
                startDate,
                endDate,
                transaction.getId()
        );

        if (!potentialDuplicates.isEmpty()) {
            log.warn("Potential duplicate detected for transaction {}: {} similar transactions found within {} minutes",
                    transaction.getNormalizedReference(), potentialDuplicates.size(), DUPLICATE_WINDOW_MINUTES);

            context.put("potentialDuplicates", potentialDuplicates);
            context.put("duplicateCount", potentialDuplicates.size());
            context.put("duplicateWindowMinutes", DUPLICATE_WINDOW_MINUTES);
            context.put("isDuplicateWarning", true);

            // Add details for each duplicate
            List<Map<String, Object>> duplicateDetails = potentialDuplicates.stream()
                    .map(dup -> Map.of(
                            "id", dup.getId().toString(),
                            "reference", dup.getNormalizedReference() != null ? dup.getNormalizedReference() : "N/A",
                            "amount", dup.getAmount(),
                            "timestamp", dup.getTimestamp().toString(),
                            "timeDifferenceMinutes", java.time.Duration.between(transaction.getTimestamp(), dup.getTimestamp()).toMinutes()
                    ))
                    .map(m -> (Map<String, Object>) m)
                    .toList();
            
            context.put("duplicateDetails", duplicateDetails);

            return true; // Duplicate found - apply penalty
        }

        return false;
    }
}
