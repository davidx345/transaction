package com.fintech.recon.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts payment references from bank narration/description fields.
 * Nigerian bank statements often embed references in narration text.
 */
@Component
@Slf4j
public class ReferenceExtractor {
    
    // Common reference patterns found in Nigerian bank narrations
    private static final List<Pattern> REFERENCE_PATTERNS = List.of(
        // Paystack format: PSK_xxx or PS-xxx
        Pattern.compile("(?:PSK_|PS-)([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
        
        // Flutterwave format: FLW-xxx
        Pattern.compile("FLW-([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE),
        
        // Generic transaction reference: TXN/xxx, REF/xxx
        Pattern.compile("(?:TXN|REF|TRANS)[/:-]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        
        // NIP/NIBSS transfer reference (16-20 alphanumeric)
        Pattern.compile("\\b([A-Z0-9]{16,20})\\b"),
        
        // Bank-specific prefixes
        Pattern.compile("(?:GTB|ACC|ZEN|FBN|UBA|STB|ECO|FID|UNB)-([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE),
        
        // Session ID format (common in NIBSS transactions)
        Pattern.compile("SESSION\\s*(?:ID)?[:-]?\\s*([0-9]+)", Pattern.CASE_INSENSITIVE),
        
        // Merchant reference
        Pattern.compile("MERCHANT\\s*REF[:-]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        
        // Order ID
        Pattern.compile("ORDER[:-]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        
        // Invoice number
        Pattern.compile("INV(?:OICE)?[:-]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE),
        
        // Generic alphanumeric (8+ chars, mixed letters and numbers)
        Pattern.compile("\\b([A-Za-z][A-Za-z0-9_-]{7,}[0-9])\\b"),
        Pattern.compile("\\b([0-9][A-Za-z0-9_-]{7,}[A-Za-z])\\b")
    );
    
    // Common narration noise words to ignore
    private static final List<String> NOISE_WORDS = List.of(
        "TRANSFER", "FROM", "TO", "PAYMENT", "DEBIT", "CREDIT", "ALERT",
        "MOBILE", "USSD", "ATM", "POS", "WEB", "ONLINE", "NIBSS", "NIP",
        "CHARGE", "FEE", "VAT", "STAMP", "DUTY", "NIGERIA", "LIMITED",
        "BANK", "ACCOUNT", "CUSTOMER", "MERCHANT"
    );
    
    /**
     * Extract the most likely reference from a narration field
     */
    public Optional<String> extractReference(String narration) {
        if (narration == null || narration.trim().isEmpty()) {
            return Optional.empty();
        }
        
        List<ReferenceCandidate> candidates = new ArrayList<>();
        
        // Try all patterns
        for (Pattern pattern : REFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(narration);
            while (matcher.find()) {
                String ref = matcher.group(0);
                String extracted = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                
                // Skip if it looks like noise
                if (isNoise(extracted)) {
                    continue;
                }
                
                // Calculate confidence score
                int score = calculateConfidence(ref, extracted, narration);
                candidates.add(new ReferenceCandidate(extracted, ref, score));
            }
        }
        
        // Return highest scoring candidate
        return candidates.stream()
            .sorted((a, b) -> Integer.compare(b.score, a.score))
            .map(c -> c.extracted)
            .findFirst();
    }
    
    /**
     * Extract all possible references from a narration (for multi-reference scenarios)
     */
    public List<String> extractAllReferences(String narration) {
        List<String> refs = new ArrayList<>();
        
        if (narration == null || narration.trim().isEmpty()) {
            return refs;
        }
        
        for (Pattern pattern : REFERENCE_PATTERNS) {
            Matcher matcher = pattern.matcher(narration);
            while (matcher.find()) {
                String extracted = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                if (!isNoise(extracted) && !refs.contains(extracted)) {
                    refs.add(extracted);
                }
            }
        }
        
        return refs;
    }
    
    /**
     * Normalize a reference for matching (remove prefixes, standardize format)
     */
    public String normalizeReference(String reference, BankFormat bankFormat) {
        if (reference == null || reference.isEmpty()) {
            return "";
        }
        
        String normalized = reference.trim().toUpperCase();
        
        // Remove bank-specific prefixes
        if (bankFormat != null && !bankFormat.getReferencePrefix().isEmpty()) {
            String prefix = bankFormat.getReferencePrefix().toUpperCase();
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
            }
        }
        
        // Remove common prefixes
        String[] prefixes = {"GTB-", "ACC-", "ZEN-", "FBN-", "UBA-", "STB-", "ECO-", "FID-", "UNB-", "PSK_", "PS-", "FLW-"};
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix.toUpperCase())) {
                normalized = normalized.substring(prefix.length());
                break;
            }
        }
        
        // Remove dashes, underscores, spaces
        normalized = normalized.replaceAll("[\\-_\\s]", "");
        
        return normalized;
    }
    
    /**
     * Check if two references match (with normalization)
     */
    public boolean referencesMatch(String ref1, String ref2) {
        String norm1 = normalizeReference(ref1, null);
        String norm2 = normalizeReference(ref2, null);
        
        if (norm1.isEmpty() || norm2.isEmpty()) {
            return false;
        }
        
        // Exact match
        if (norm1.equals(norm2)) {
            return true;
        }
        
        // One contains the other (partial match)
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            // Only if the shorter one is at least 6 chars
            int minLen = Math.min(norm1.length(), norm2.length());
            return minLen >= 6;
        }
        
        return false;
    }
    
    /**
     * Calculate confidence score for a reference candidate
     */
    private int calculateConfidence(String fullMatch, String extracted, String narration) {
        int score = 0;
        
        // Length score (longer is usually better, up to a point)
        if (extracted.length() >= 8) score += 10;
        if (extracted.length() >= 12) score += 10;
        if (extracted.length() >= 16) score += 5;
        
        // Has known prefix
        if (fullMatch.matches("(?i)(PSK_|PS-|FLW-|GTB-|ACC-|ZEN-|FBN-|UBA-).*")) {
            score += 30;
        }
        
        // Mixed alphanumeric (more likely to be a reference)
        if (extracted.matches(".*[A-Za-z].*") && extracted.matches(".*[0-9].*")) {
            score += 20;
        }
        
        // Near keywords like "REF", "REFERENCE", "TRANSACTION"
        int keywordPos = findNearestKeyword(narration, fullMatch);
        if (keywordPos >= 0 && keywordPos < 20) {
            score += 15;
        }
        
        // Penalize all-numeric (could be account number)
        if (extracted.matches("[0-9]+")) {
            score -= 10;
            // Unless it's 16+ digits (session ID)
            if (extracted.length() >= 16) score += 15;
        }
        
        return score;
    }
    
    /**
     * Find distance to nearest reference-indicating keyword
     */
    private int findNearestKeyword(String narration, String match) {
        String[] keywords = {"REF", "REFERENCE", "TRANS", "TXN", "SESSION", "ORDER", "INVOICE"};
        String upper = narration.toUpperCase();
        int matchPos = upper.indexOf(match.toUpperCase());
        
        if (matchPos < 0) return -1;
        
        int nearestDist = Integer.MAX_VALUE;
        for (String keyword : keywords) {
            int kwPos = upper.indexOf(keyword);
            if (kwPos >= 0) {
                int dist = Math.abs(matchPos - kwPos);
                nearestDist = Math.min(nearestDist, dist);
            }
        }
        
        return nearestDist == Integer.MAX_VALUE ? -1 : nearestDist;
    }
    
    /**
     * Check if a string is a common noise word
     */
    private boolean isNoise(String text) {
        if (text == null || text.length() < 4) return true;
        
        String upper = text.toUpperCase();
        for (String noise : NOISE_WORDS) {
            if (upper.equals(noise)) return true;
        }
        
        // All same character
        if (text.chars().distinct().count() <= 2) return true;
        
        return false;
    }
    
    /**
     * Internal class to hold reference candidates with scores
     */
    private static class ReferenceCandidate {
        final String extracted;
        final String fullMatch;
        final int score;
        
        ReferenceCandidate(String extracted, String fullMatch, int score) {
            this.extracted = extracted;
            this.fullMatch = fullMatch;
            this.score = score;
        }
    }
}
