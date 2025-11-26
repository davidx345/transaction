package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Same Day Match Rule
 * Awards higher points when transactions match on the same day.
 * This is more confident than date range matches.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SameDayMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    @Override
    public String getRuleName() {
        return "SameDayMatch";
    }

    @Override
    public int getWeight() {
        return config.getWeights().getSameDayMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        if (transaction.getTimestamp() == null) {
            return false;
        }

        // Get start and end of the same day
        var startOfDay = transaction.getTimestamp().toLocalDate().atStartOfDay();
        var endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        // Find transactions from the same day in ledger
        List<Transaction> sameDayTransactions = transactionRepository
                .findBySourceAndTimestampBetween("ledger", startOfDay, endOfDay);

        if (!sameDayTransactions.isEmpty()) {
            log.debug("Same day transactions found: {} candidates", sameDayTransactions.size());
            
            context.put("sameDayMatches", sameDayTransactions);
            context.put("sameDayMatchCount", sameDayTransactions.size());
            context.put("transactionDate", transaction.getTimestamp().toLocalDate());

            return true;
        }

        return false;
    }
}
