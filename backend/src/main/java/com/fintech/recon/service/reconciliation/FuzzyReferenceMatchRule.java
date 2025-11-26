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
 * Fuzzy Reference Match Rule
 * Matches transaction references that may have variations:
 * - Different separators (PSK_TXN001 vs PSK-TXN-001)
 * - Case differences (psk_txn001 vs PSK_TXN001)
 * - Missing prefixes (TXN001 vs PSK_TXN001)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FuzzyReferenceMatchRule implements ReconciliationRule {

    private final TransactionRepository transactionRepository;
    private final ReconciliationConfig config;

    @Override
    public String getRuleName() {
        return "FuzzyReferenceMatch";
    }

    @Override
    public int getWeight() {
        return config.getWeights().getFuzzyReferenceMatch();
    }

    @Override
    public boolean evaluate(Transaction transaction, Map<String, Object> context) {
        String originalRef = transaction.getNormalizedReference();
        if (originalRef == null || originalRef.isEmpty()) {
            return false;
        }

        // Normalize the reference: remove separators, lowercase
        String cleanedRef = normalizeReference(originalRef);
        
        // Search in ledger source for fuzzy matches
        List<Transaction> potentialMatches = transactionRepository.findByFuzzyReference("ledger", cleanedRef);

        // Filter out exact matches (handled by ExactMatchRule)
        potentialMatches = potentialMatches.stream()
                .filter(t -> !t.getNormalizedReference().equalsIgnoreCase(originalRef))
                .filter(t -> calculateSimilarity(cleanedRef, normalizeReference(t.getNormalizedReference())) >= 0.8)
                .toList();

        if (!potentialMatches.isEmpty()) {
            log.debug("Fuzzy match found for {} -> {} candidates", originalRef, potentialMatches.size());
            context.put("fuzzyMatches", potentialMatches);
            context.put("fuzzyMatchCount", potentialMatches.size());
            
            // Store best match
            Transaction bestMatch = potentialMatches.stream()
                    .max((a, b) -> Double.compare(
                            calculateSimilarity(cleanedRef, normalizeReference(a.getNormalizedReference())),
                            calculateSimilarity(cleanedRef, normalizeReference(b.getNormalizedReference()))
                    ))
                    .orElse(null);
            
            if (bestMatch != null) {
                context.put("fuzzyMatchedTransaction", bestMatch);
                context.put("fuzzyMatchSimilarity", 
                        calculateSimilarity(cleanedRef, normalizeReference(bestMatch.getNormalizedReference())));
            }
            
            return true;
        }

        return false;
    }

    /**
     * Normalize reference by removing separators and converting to lowercase
     */
    public static String normalizeReference(String reference) {
        if (reference == null) return "";
        return reference
                .replaceAll("[\\-_\\s]", "")  // Remove dashes, underscores, spaces
                .toLowerCase()
                .trim();
    }

    /**
     * Calculate similarity between two strings using Levenshtein distance
     * Returns value between 0 (no match) and 1 (exact match)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 1.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
