package com.fintech.recon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for reconciliation matching tolerances and rules.
 * These values can be overridden in application.yml or application-prod.yml
 */
@Configuration
@ConfigurationProperties(prefix = "reconciliation")
@Data
public class ReconciliationConfig {

    /**
     * Amount tolerance settings
     */
    private AmountTolerance amount = new AmountTolerance();

    /**
     * Date matching settings
     */
    private DateTolerance date = new DateTolerance();

    /**
     * Confidence score thresholds
     */
    private ConfidenceThresholds confidence = new ConfidenceThresholds();

    /**
     * Rule weights (points assigned when rule matches)
     */
    private RuleWeights weights = new RuleWeights();

    /**
     * Bank-specific fee configurations
     */
    private Map<String, BankFeeConfig> bankFees = new HashMap<>();

    @Data
    public static class AmountTolerance {
        /**
         * Default percentage tolerance for amount matching (e.g., 0.02 = 2%)
         */
        private BigDecimal defaultPercentage = new BigDecimal("0.02");

        /**
         * Maximum absolute tolerance in currency units
         */
        private BigDecimal maxAbsolute = new BigDecimal("100.00");

        /**
         * Minimum transaction amount to apply percentage tolerance
         * Below this, use absolute tolerance
         */
        private BigDecimal minAmountForPercentage = new BigDecimal("1000.00");
    }

    @Data
    public static class DateTolerance {
        /**
         * Number of days before transaction date to search for matches
         */
        private int daysBefore = 1;

        /**
         * Number of days after transaction date to search for matches
         * (T+1, T+2 settlements)
         */
        private int daysAfter = 3;

        /**
         * Whether to consider weekends/holidays in date matching
         */
        private boolean skipWeekends = true;
    }

    @Data
    public static class ConfidenceThresholds {
        /**
         * Score >= this value = AUTO_MATCH (no review needed)
         */
        private int autoMatchThreshold = 95;

        /**
         * Score >= this value = HIGH_CONFIDENCE (quick review)
         */
        private int highConfidenceThreshold = 85;

        /**
         * Score >= this value = MEDIUM_CONFIDENCE (standard review)
         */
        private int mediumConfidenceThreshold = 70;

        /**
         * Score >= this value = LOW_CONFIDENCE (detailed review)
         */
        private int lowConfidenceThreshold = 40;

        /**
         * Score below lowConfidenceThreshold = NO_MATCH
         */
    }

    @Data
    public static class RuleWeights {
        /**
         * Points for exact reference match
         */
        private int exactReferenceMatch = 40;

        /**
         * Points for fuzzy reference match (normalized)
         */
        private int fuzzyReferenceMatch = 30;

        /**
         * Points for exact amount match
         */
        private int exactAmountMatch = 30;

        /**
         * Points for amount within tolerance
         */
        private int toleranceAmountMatch = 20;

        /**
         * Points for same-day transaction
         */
        private int sameDayMatch = 20;

        /**
         * Points for transaction within date window
         */
        private int dateRangeMatch = 10;

        /**
         * Points for matching status
         */
        private int statusMatch = 10;

        /**
         * Penalty for potential duplicate
         */
        private int duplicatePenalty = -20;
    }

    @Data
    public static class BankFeeConfig {
        /**
         * Bank name
         */
        private String bankName;

        /**
         * Percentage fee charged by bank
         */
        private BigDecimal feePercentage = BigDecimal.ZERO;

        /**
         * Flat fee amount
         */
        private BigDecimal flatFee = BigDecimal.ZERO;

        /**
         * Maximum fee cap
         */
        private BigDecimal maxFee = new BigDecimal("2000.00");

        /**
         * Whether VAT is applied on fees
         */
        private boolean vatApplied = true;

        /**
         * VAT percentage (Nigerian VAT is 7.5%)
         */
        private BigDecimal vatPercentage = new BigDecimal("0.075");
    }

    /**
     * Calculate expected amount after bank fees
     */
    public BigDecimal calculateExpectedSettlement(BigDecimal originalAmount, String bankCode) {
        BankFeeConfig feeConfig = bankFees.get(bankCode.toLowerCase());
        if (feeConfig == null) {
            return originalAmount;
        }

        BigDecimal fee = originalAmount.multiply(feeConfig.getFeePercentage())
                .add(feeConfig.getFlatFee());

        if (feeConfig.getMaxFee() != null && fee.compareTo(feeConfig.getMaxFee()) > 0) {
            fee = feeConfig.getMaxFee();
        }

        if (feeConfig.isVatApplied()) {
            fee = fee.add(fee.multiply(feeConfig.getVatPercentage()));
        }

        return originalAmount.subtract(fee);
    }
}
