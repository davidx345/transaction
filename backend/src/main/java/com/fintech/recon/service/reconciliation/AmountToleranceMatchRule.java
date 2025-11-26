package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Amount Tolerance Match Rule
 * Matches transactions where amounts are within a configurable tolerance.
 * This handles cases where:
 * - Bank fees are deducted from settlements
 * - Small rounding differences occur
 * - Currency conversion variations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AmountToleranceMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    @Override
    public String getRuleName() {
        return "AmountToleranceMatch";
    }

    @Override
    public int getWeight() {
        return config.getWeights().getToleranceAmountMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        BigDecimal originalAmount = transaction.getAmount();
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        // Calculate tolerance range
        BigDecimal tolerance = calculateTolerance(originalAmount);
        BigDecimal minAmount = originalAmount.subtract(tolerance);
        BigDecimal maxAmount = originalAmount.add(tolerance);

        // Don't go below zero
        if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
            minAmount = BigDecimal.ZERO;
        }

        log.debug("Searching for amounts in range {} - {} (tolerance: {})", minAmount, maxAmount, tolerance);

        // Find transactions within tolerance range
        List<Transaction> potentialMatches = transactionRepository.findByAmountRange("ledger", minAmount, maxAmount);

        // Filter out exact matches (handled by ExactMatchRule)
        potentialMatches = potentialMatches.stream()
                .filter(t -> t.getAmount().compareTo(originalAmount) != 0)
                .toList();

        if (!potentialMatches.isEmpty()) {
            log.debug("Amount tolerance match found: {} candidates within {} tolerance", 
                    potentialMatches.size(), tolerance);
            
            context.put("toleranceMatches", potentialMatches);
            context.put("toleranceMatchCount", potentialMatches.size());
            context.put("toleranceUsed", tolerance);

            // Find closest match
            Transaction closestMatch = potentialMatches.stream()
                    .min((a, b) -> {
                        BigDecimal diffA = a.getAmount().subtract(originalAmount).abs();
                        BigDecimal diffB = b.getAmount().subtract(originalAmount).abs();
                        return diffA.compareTo(diffB);
                    })
                    .orElse(null);

            if (closestMatch != null) {
                BigDecimal difference = closestMatch.getAmount().subtract(originalAmount);
                BigDecimal percentageDiff = difference.divide(originalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                
                context.put("toleranceMatchedTransaction", closestMatch);
                context.put("amountDifference", difference);
                context.put("percentageDifference", percentageDiff);
            }

            return true;
        }

        return false;
    }

    /**
     * Calculate tolerance based on amount and configuration
     */
    private BigDecimal calculateTolerance(BigDecimal amount) {
        ReconciliationConfig.AmountTolerance toleranceConfig = config.getAmount();

        // For small amounts, use absolute tolerance
        if (amount.compareTo(toleranceConfig.getMinAmountForPercentage()) < 0) {
            return toleranceConfig.getMaxAbsolute();
        }

        // Calculate percentage-based tolerance
        BigDecimal percentageTolerance = amount.multiply(toleranceConfig.getDefaultPercentage());

        // Cap at maximum absolute tolerance
        if (percentageTolerance.compareTo(toleranceConfig.getMaxAbsolute()) > 0) {
            return toleranceConfig.getMaxAbsolute();
        }

        return percentageTolerance;
    }

    /**
     * Calculate expected settlement amount after bank fees
     */
    public BigDecimal calculateExpectedSettlement(BigDecimal originalAmount, String bankCode) {
        return config.calculateExpectedSettlement(originalAmount, bankCode);
    }
}
