package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Date Range Match Rule
 * Matches transactions within a configurable date window.
 * This handles cases where:
 * - Bank settlements are T+1 or T+2
 * - Different timezone processing
 * - Weekend/holiday delays
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DateRangeMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    @Override
    public String getRuleName() {
        return "DateRangeMatch";
    }

    @Override
    public int getWeight() {
        return config.getWeights().getDateRangeMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        LocalDateTime transactionDate = transaction.getTimestamp();
        if (transactionDate == null) {
            return false;
        }

        ReconciliationConfig.DateTolerance dateTolerance = config.getDate();

        // Calculate date range
        LocalDateTime startDate = calculateStartDate(transactionDate, dateTolerance);
        LocalDateTime endDate = calculateEndDate(transactionDate, dateTolerance);

        log.debug("Searching for transactions between {} and {}", startDate, endDate);

        // Find transactions within date range
        List<Transaction> potentialMatches = transactionRepository
                .findBySourceAndTimestampBetween("ledger", startDate, endDate);

        // Filter out same-day matches (those get higher score from SameDayMatchRule)
        potentialMatches = potentialMatches.stream()
                .filter(t -> !isSameDay(t.getTimestamp(), transactionDate))
                .toList();

        if (!potentialMatches.isEmpty()) {
            log.debug("Date range match found: {} candidates within {} days before to {} days after",
                    potentialMatches.size(), dateTolerance.getDaysBefore(), dateTolerance.getDaysAfter());

            context.put("dateRangeMatches", potentialMatches);
            context.put("dateRangeMatchCount", potentialMatches.size());
            context.put("dateRangeStart", startDate);
            context.put("dateRangeEnd", endDate);

            // Find closest match by date
            Transaction closestMatch = potentialMatches.stream()
                    .min((a, b) -> {
                        long diffA = Math.abs(java.time.Duration.between(a.getTimestamp(), transactionDate).toDays());
                        long diffB = Math.abs(java.time.Duration.between(b.getTimestamp(), transactionDate).toDays());
                        return Long.compare(diffA, diffB);
                    })
                    .orElse(null);

            if (closestMatch != null) {
                long daysDifference = java.time.Duration.between(transactionDate, closestMatch.getTimestamp()).toDays();
                context.put("dateRangeMatchedTransaction", closestMatch);
                context.put("daysDifference", daysDifference);
            }

            return true;
        }

        return false;
    }

    /**
     * Calculate start date considering weekends if configured
     */
    private LocalDateTime calculateStartDate(LocalDateTime date, ReconciliationConfig.DateTolerance tolerance) {
        LocalDateTime startDate = date.minusDays(tolerance.getDaysBefore());
        
        if (tolerance.isSkipWeekends()) {
            // Extend start date if it falls on weekend
            while (isWeekend(startDate)) {
                startDate = startDate.minusDays(1);
            }
        }
        
        return startDate.withHour(0).withMinute(0).withSecond(0);
    }

    /**
     * Calculate end date considering weekends if configured
     */
    private LocalDateTime calculateEndDate(LocalDateTime date, ReconciliationConfig.DateTolerance tolerance) {
        LocalDateTime endDate = date.plusDays(tolerance.getDaysAfter());
        
        if (tolerance.isSkipWeekends()) {
            // Extend end date if it falls on weekend
            while (isWeekend(endDate)) {
                endDate = endDate.plusDays(1);
            }
        }
        
        return endDate.withHour(23).withMinute(59).withSecond(59);
    }

    private boolean isWeekend(LocalDateTime date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null || date2 == null) return false;
        return date1.toLocalDate().equals(date2.toLocalDate());
    }
}
