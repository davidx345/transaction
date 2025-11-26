package com.fintech.recon.service.reconciliation;

import com.fintech.recon.config.ReconciliationConfig;
import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.domain.valueobject.MatchResult;
import com.fintech.recon.infrastructure.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final ReconciliationConfig config;
    private final List<ReconciliationRule> rules;

    /**
     * Reconcile a transaction and return detailed match result
     */
    public Reconciliation reconcile(Transaction transaction) {
        log.info("Reconciling transaction: {}", transaction.getNormalizedReference());

        // Build context for rule evaluation
        Map<String, Object> context = new HashMap<>();
        context.put("sourceTransaction", transaction);

        int totalScore = 0;
        List<Map<String, Object>> rulesFired = new ArrayList<>();
        List<MatchResult.RuleResult> ruleResults = new ArrayList<>();

        // Apply all rules and collect results
        for (ReconciliationRule rule : rules) {
            boolean matched = false;
            try {
                matched = rule.evaluate(transaction, context);
            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getRuleName(), e.getMessage());
            }

            int points = matched ? rule.getWeight() : 0;
            totalScore += points;

            // Build rule result for detailed analysis
            MatchResult.RuleResult ruleResult = MatchResult.RuleResult.builder()
                    .ruleName(rule.getRuleName())
                    .matched(matched)
                    .pointsAwarded(points)
                    .maxPoints(Math.abs(rule.getWeight()))
                    .description(buildRuleDescription(rule.getRuleName(), matched, context))
                    .details(extractRuleDetails(rule.getRuleName(), context))
                    .build();
            ruleResults.add(ruleResult);

            if (matched) {
                Map<String, Object> ruleInfo = new HashMap<>();
                ruleInfo.put("rule", rule.getRuleName());
                ruleInfo.put("weight", rule.getWeight());
                ruleInfo.put("description", ruleResult.getDescription());
                rulesFired.add(ruleInfo);
            }
        }

        // Ensure score is within bounds
        totalScore = Math.max(0, Math.min(100, totalScore));

        // Determine state based on score and config thresholds
        String state = determineState(totalScore, context);

        // Build match analysis
        MatchResult.MatchAnalysis analysis = buildMatchAnalysis(context);

        // Add analysis to context for storage
        context.put("matchAnalysis", analysis);
        context.put("ruleResults", ruleResults);

        // Build warnings list
        List<String> warnings = buildWarnings(context);

        // Create reconciliation result
        Reconciliation reconciliation = Reconciliation.builder()
                .transactionRef(transaction.getNormalizedReference())
                .runId(UUID.randomUUID().toString())
                .confidenceScore(totalScore)
                .rulesFired(rulesFired)
                .state(state)
                .build();

        // Add initial audit entry
        Reconciliation.AuditEntry auditEntry = Reconciliation.AuditEntry.builder()
                .timestamp(java.time.LocalDateTime.now())
                .action("RECONCILED")
                .actor("system")
                .reason("Auto-reconciliation with score: " + totalScore + "% (" + state + ")")
                .build();
        reconciliation.getAuditTrail().add(auditEntry);

        // Persist result
        Reconciliation saved = reconciliationRepository.save(reconciliation);

        log.info("Reconciliation complete for {}: {} (score: {})", 
                transaction.getNormalizedReference(), state, totalScore);

        return saved;
    }

    /**
     * Determine state based on score and configuration thresholds
     */
    private String determineState(int score, Map<String, Object> context) {
        ReconciliationConfig.ConfidenceThresholds thresholds = config.getConfidence();

        // Check for duplicate warning
        Boolean isDuplicateWarning = (Boolean) context.get("isDuplicateWarning");
        if (Boolean.TRUE.equals(isDuplicateWarning)) {
            return "DUPLICATE_WARNING";
        }

        if (score >= thresholds.getAutoMatchThreshold()) {
            return "AUTO_MATCHED";
        }
        if (score >= thresholds.getHighConfidenceThreshold()) {
            return "HIGH_CONFIDENCE";
        }
        if (score >= thresholds.getMediumConfidenceThreshold()) {
            return "MEDIUM_CONFIDENCE";
        }
        if (score >= thresholds.getLowConfidenceThreshold()) {
            return "LOW_CONFIDENCE";
        }
        return "NO_MATCH";
    }

    /**
     * Build human-readable description for each rule
     */
    private String buildRuleDescription(String ruleName, boolean matched, Map<String, Object> context) {
        if (!matched) {
            return "No match found";
        }

        return switch (ruleName) {
            case "ExactMatch" -> "Exact reference and amount match found in ledger";
            case "FuzzyReferenceMatch" -> {
                Double similarity = (Double) context.get("fuzzyMatchSimilarity");
                yield String.format("Similar reference found (%.1f%% similarity)", 
                        similarity != null ? similarity * 100 : 0);
            }
            case "AmountToleranceMatch" -> {
                BigDecimal diff = (BigDecimal) context.get("percentageDifference");
                yield String.format("Amount within tolerance (%.2f%% difference)", 
                        diff != null ? diff.abs() : BigDecimal.ZERO);
            }
            case "SameDayMatch" -> "Transaction found on the same day";
            case "DateRangeMatch" -> {
                Long days = (Long) context.get("daysDifference");
                yield String.format("Transaction found within %d day(s)", 
                        days != null ? Math.abs(days) : 0);
            }
            case "StatusMatch" -> "Transaction statuses match";
            case "DuplicateDetection" -> {
                Integer count = (Integer) context.get("duplicateCount");
                yield String.format("Warning: %d potential duplicate(s) detected", 
                        count != null ? count : 0);
            }
            default -> "Rule matched";
        };
    }

    /**
     * Extract relevant details for each rule result
     */
    private Map<String, Object> extractRuleDetails(String ruleName, Map<String, Object> context) {
        Map<String, Object> details = new HashMap<>();

        switch (ruleName) {
            case "FuzzyReferenceMatch" -> {
                details.put("similarity", context.get("fuzzyMatchSimilarity"));
                details.put("matchCount", context.get("fuzzyMatchCount"));
            }
            case "AmountToleranceMatch" -> {
                details.put("amountDifference", context.get("amountDifference"));
                details.put("percentageDifference", context.get("percentageDifference"));
                details.put("toleranceUsed", context.get("toleranceUsed"));
            }
            case "DateRangeMatch" -> {
                details.put("daysDifference", context.get("daysDifference"));
                details.put("dateRangeStart", context.get("dateRangeStart"));
                details.put("dateRangeEnd", context.get("dateRangeEnd"));
            }
            case "DuplicateDetection" -> {
                details.put("duplicateCount", context.get("duplicateCount"));
                details.put("duplicateDetails", context.get("duplicateDetails"));
            }
            case "StatusMatch" -> {
                details.put("originalStatus", context.get("originalStatus"));
                details.put("normalizedStatus", context.get("normalizedStatus"));
                details.put("statusesMatch", context.get("statusesMatch"));
            }
        }

        return details;
    }

    /**
     * Build match analysis summary from context
     */
    private MatchResult.MatchAnalysis buildMatchAnalysis(Map<String, Object> context) {
        return MatchResult.MatchAnalysis.builder()
                // Reference analysis
                .referenceMatched(context.containsKey("matchedTransaction") || 
                                  context.containsKey("fuzzyMatchedTransaction"))
                .referenceMatchType(context.containsKey("matchedTransaction") ? "EXACT" :
                                   context.containsKey("fuzzyMatchedTransaction") ? "FUZZY" : "NONE")
                .referenceSimilarity(context.containsKey("fuzzyMatchSimilarity") ? 
                        (Double) context.get("fuzzyMatchSimilarity") : 
                        context.containsKey("matchedTransaction") ? 1.0 : 0.0)
                
                // Amount analysis
                .amountMatched(context.containsKey("matchedTransaction") || 
                               context.containsKey("toleranceMatchedTransaction"))
                .amountMatchType(context.containsKey("matchedTransaction") ? "EXACT" :
                                context.containsKey("toleranceMatchedTransaction") ? "TOLERANCE" : "NONE")
                .amountDifference((BigDecimal) context.get("amountDifference"))
                .percentageDifference((BigDecimal) context.get("percentageDifference"))
                
                // Date analysis
                .dateMatched(context.containsKey("sameDayMatches") || 
                            context.containsKey("dateRangeMatches"))
                .dateMatchType(context.containsKey("sameDayMatches") ? "SAME_DAY" :
                              context.containsKey("dateRangeMatches") ? "WITHIN_RANGE" : "NONE")
                .daysDifference((Long) context.get("daysDifference"))
                
                // Status analysis
                .statusMatched(Boolean.TRUE.equals(context.get("statusesMatch")))
                .sourceStatus((String) context.get("normalizedStatus"))
                .matchedStatus((String) context.get("matchedNormalizedStatus"))
                
                // Duplicate analysis
                .duplicateWarning(Boolean.TRUE.equals(context.get("isDuplicateWarning")))
                .duplicateCount(context.containsKey("duplicateCount") ? 
                        (Integer) context.get("duplicateCount") : 0)
                .build();
    }

    /**
     * Build warnings list from context
     */
    private List<String> buildWarnings(Map<String, Object> context) {
        List<String> warnings = new ArrayList<>();

        if (Boolean.TRUE.equals(context.get("isDuplicateWarning"))) {
            Integer count = (Integer) context.get("duplicateCount");
            warnings.add(String.format("Potential duplicate: %d similar transaction(s) found within 30 minutes", 
                    count != null ? count : 0));
        }

        BigDecimal percentDiff = (BigDecimal) context.get("percentageDifference");
        if (percentDiff != null && percentDiff.abs().compareTo(new BigDecimal("1")) > 0) {
            warnings.add(String.format("Amount difference of %.2f%% detected - verify bank fees", 
                    percentDiff.abs()));
        }

        Long daysDiff = (Long) context.get("daysDifference");
        if (daysDiff != null && Math.abs(daysDiff) > 2) {
            warnings.add(String.format("Settlement delay of %d days detected", Math.abs(daysDiff)));
        }

        return warnings;
    }
}

