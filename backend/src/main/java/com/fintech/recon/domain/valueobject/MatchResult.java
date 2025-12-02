package com.fintech.recon.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed result of a reconciliation match attempt.
 * Contains all the information about why a transaction matched or didn't match.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    /**
     * Unique identifier for this match result
     */
    private UUID id;

    /**
     * The transaction being reconciled
     */
    private TransactionSummary sourceTransaction;

    /**
     * The best matched transaction (if any)
     */
    private TransactionSummary matchedTransaction;

    /**
     * Overall confidence score (0-100)
     */
    private int confidenceScore;

    /**
     * Match status
     */
    private MatchStatus status;

    /**
     * Detailed breakdown of each rule that was evaluated
     */
    @Builder.Default
    private List<RuleResult> ruleResults = new ArrayList<>();

    /**
     * Overall match analysis
     */
    private MatchAnalysis analysis;

    /**
     * Warnings and recommendations
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Timestamp of the match attempt
     */
    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public enum MatchStatus {
        AUTO_MATCHED,      // Score >= 95, no review needed
        HIGH_CONFIDENCE,   // Score >= 85, quick review
        MEDIUM_CONFIDENCE, // Score >= 70, standard review
        LOW_CONFIDENCE,    // Score >= 40, detailed review
        NO_MATCH,          // Score < 40, likely unmatched
        DUPLICATE_WARNING  // Potential duplicate detected
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionSummary {
        private UUID id;
        private String reference;
        private String source;
        private BigDecimal amount;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResult {
        /**
         * Name of the rule
         */
        private String ruleName;

        /**
         * Whether the rule matched
         */
        private boolean matched;

        /**
         * Points awarded (can be negative for penalties)
         */
        private int pointsAwarded;

        /**
         * Maximum possible points for this rule
         */
        private int maxPoints;

        /**
         * Human-readable description of why the rule matched/didn't match
         */
        private String description;

        /**
         * Additional details about the match
         */
        private Map<String, Object> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchAnalysis {
        /**
         * Whether reference matched (exact or fuzzy)
         */
        private boolean referenceMatched;

        /**
         * Reference match type
         */
        private String referenceMatchType; // EXACT, FUZZY, NONE

        /**
         * Reference similarity score (0-1)
         */
        private double referenceSimilarity;

        /**
         * Whether amount matched (exact or within tolerance)
         */
        private boolean amountMatched;

        /**
         * Amount match type
         */
        private String amountMatchType; // EXACT, TOLERANCE, NONE

        /**
         * Amount difference (if any)
         */
        private BigDecimal amountDifference;

        /**
         * Percentage difference in amount
         */
        private BigDecimal percentageDifference;

        /**
         * Whether date matched (same day or within range)
         */
        private boolean dateMatched;

        /**
         * Date match type
         */
        private String dateMatchType; // SAME_DAY, WITHIN_RANGE, NONE

        /**
         * Days difference (if any)
         */
        private Long daysDifference;

        /**
         * Whether status matched
         */
        private boolean statusMatched;

        /**
         * Normalized source status
         */
        private String sourceStatus;

        /**
         * Normalized matched status
         */
        private String matchedStatus;

        /**
         * Whether potential duplicates were found
         */
        private boolean duplicateWarning;

        /**
         * Number of potential duplicates
         */
        private int duplicateCount;
    }

    /**
     * Add a rule result to the list
     */
    public void addRuleResult(RuleResult result) {
        if (this.ruleResults == null) {
            this.ruleResults = new ArrayList<>();
        }
        this.ruleResults.add(result);
    }

    /**
     * Add a warning
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * Get summary for display
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Match Result: ").append(status).append(" (").append(confidenceScore).append("%)");
        
        if (matchedTransaction != null) {
            sb.append(" -> Matched with ").append(matchedTransaction.getReference());
        }
        
        if (!warnings.isEmpty()) {
            sb.append(" [").append(warnings.size()).append(" warning(s)]");
        }
        
        return sb.toString();
    }
}
