package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Status Match Rule
 * Awards points when transaction statuses indicate a successful match.
 * Maps different status formats across providers/banks to normalized statuses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusMatchRule implements ReconciliationRule {

    private final ReconciliationConfig config;

    // Status mappings for normalization
    private static final Set<String> SUCCESS_STATUSES = Set.of(
            "success", "successful", "completed", "settled", "approved",
            "paid", "confirmed", "processed", "done", "ok"
    );

    private static final Set<String> PENDING_STATUSES = Set.of(
            "pending", "processing", "in_progress", "awaiting", "initiated",
            "queued", "submitted"
    );

    private static final Set<String> FAILED_STATUSES = Set.of(
            "failed", "failure", "declined", "rejected", "cancelled",
            "reversed", "refunded", "error", "timeout"
    );

    @Override
    public String getRuleName() {
        return "StatusMatch";
    }

    @Override
    public int getWeight() {
        return config.getWeights().getStatusMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        String status = transaction.getStatus();
        if (status == null || status.isEmpty()) {
            return false;
        }

        String normalizedStatus = normalizeStatus(status);
        context.put("normalizedStatus", normalizedStatus);
        context.put("originalStatus", status);

        // Check if there's a matched transaction in context to compare status
        Transaction matchedTransaction = (Transaction) context.get("matchedTransaction");
        if (matchedTransaction == null) {
            matchedTransaction = (Transaction) context.get("fuzzyMatchedTransaction");
        }
        if (matchedTransaction == null) {
            matchedTransaction = (Transaction) context.get("toleranceMatchedTransaction");
        }

        if (matchedTransaction != null && matchedTransaction.getStatus() != null) {
            String matchedNormalizedStatus = normalizeStatus(matchedTransaction.getStatus());
            context.put("matchedTransactionStatus", matchedTransaction.getStatus());
            context.put("matchedNormalizedStatus", matchedNormalizedStatus);

            boolean statusesMatch = normalizedStatus.equals(matchedNormalizedStatus);
            context.put("statusesMatch", statusesMatch);

            if (statusesMatch) {
                log.debug("Status match: {} ({}) = {} ({})",
                        status, normalizedStatus,
                        matchedTransaction.getStatus(), matchedNormalizedStatus);
                return true;
            }
        }

        // If we have a success status, give partial credit
        if ("SUCCESS".equals(normalizedStatus)) {
            context.put("hasSuccessStatus", true);
            return true;
        }

        return false;
    }

    /**
     * Normalize status to one of: SUCCESS, PENDING, FAILED, UNKNOWN
     */
    public static String normalizeStatus(String status) {
        if (status == null) return "UNKNOWN";
        
        String lowerStatus = status.toLowerCase().trim();

        if (SUCCESS_STATUSES.stream().anyMatch(lowerStatus::contains)) {
            return "SUCCESS";
        }
        if (PENDING_STATUSES.stream().anyMatch(lowerStatus::contains)) {
            return "PENDING";
        }
        if (FAILED_STATUSES.stream().anyMatch(lowerStatus::contains)) {
            return "FAILED";
        }

        return "UNKNOWN";
    }
}
