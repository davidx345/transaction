package com.fintech.recon.service;

import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.domain.Transaction;
import com.fintech.recon.dto.ReportDtos.*;
import com.fintech.recon.infrastructure.ReconciliationRepository;
import com.fintech.recon.infrastructure.TransactionRepository;
import com.fintech.recon.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating reconciliation reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get transactions for current user
     */
    private List<Transaction> getCurrentUserTransactions() {
        UUID userId = securityUtils.getCurrentUserId();
        if (userId != null) {
            return transactionRepository.findByUserId(userId);
        }
        return transactionRepository.findAll();
    }

    /**
     * Get reconciliations for current user
     */
    private List<Reconciliation> getCurrentUserReconciliations() {
        UUID userId = securityUtils.getCurrentUserId();
        if (userId != null) {
            return reconciliationRepository.findByUserId(userId);
        }
        return reconciliationRepository.findAll();
    }

    /**
     * Generate daily summary report
     */
    public DailySummaryReport generateDailySummary(LocalDate date) {
        log.info("Generating daily summary report for {}", date);
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        // Get all transactions for the day - filtered by current user
        List<Transaction> allTransactions = getCurrentUserTransactions().stream()
            .filter(t -> t.getTimestamp() != null)
            .filter(t -> !t.getTimestamp().isBefore(startOfDay) && !t.getTimestamp().isAfter(endOfDay))
            .toList();
        
        // Get all reconciliations - filtered by current user
        List<Reconciliation> reconciliations = getCurrentUserReconciliations();
        Set<String> matchedRefs = reconciliations.stream()
            .filter(r -> "MATCHED".equals(r.getState()) || "AUTO_MATCHED".equals(r.getState()))
            .map(Reconciliation::getTransactionRef)
            .collect(Collectors.toSet());
        
        Set<String> disputedRefs = reconciliations.stream()
            .filter(r -> "DISPUTED".equals(r.getState()))
            .map(Reconciliation::getTransactionRef)
            .collect(Collectors.toSet());
        
        // Calculate statistics
        int total = allTransactions.size();
        int matched = (int) allTransactions.stream()
            .filter(t -> matchedRefs.contains(t.getNormalizedReference()))
            .count();
        int disputed = (int) allTransactions.stream()
            .filter(t -> disputedRefs.contains(t.getNormalizedReference()))
            .count();
        int pending = (int) allTransactions.stream()
            .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
            .count();
        int unmatched = total - matched;
        
        BigDecimal totalAmount = allTransactions.stream()
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal matchedAmount = allTransactions.stream()
            .filter(t -> matchedRefs.contains(t.getNormalizedReference()))
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal unmatchedAmount = totalAmount.subtract(matchedAmount);
        
        // Calculate match rate
        double matchRate = total > 0 ? (double) matched / total * 100 : 0;
        
        // Auto match rate (HIGH_CONFIDENCE matches)
        long autoMatched = reconciliations.stream()
            .filter(r -> "AUTO_MATCHED".equals(r.getState()))
            .count();
        double autoMatchRate = matched > 0 ? (double) autoMatched / matched * 100 : 0;
        
        // Source breakdown
        List<SourceBreakdown> sourceBreakdowns = calculateSourceBreakdowns(allTransactions, matchedRefs);
        
        // Hourly distribution
        Map<Integer, Integer> hourlyDist = allTransactions.stream()
            .filter(t -> t.getTimestamp() != null)
            .collect(Collectors.groupingBy(
                t -> t.getTimestamp().getHour(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Top discrepancies
        List<DiscrepancyItem> topDiscrepancies = findTopDiscrepancies(allTransactions, reconciliations, 10);
        
        return DailySummaryReport.builder()
            .reportDate(date)
            .generatedAt(LocalDateTime.now())
            .totalTransactions(total)
            .matchedTransactions(matched)
            .unmatchedTransactions(unmatched)
            .pendingTransactions(pending)
            .disputedTransactions(disputed)
            .totalAmount(totalAmount)
            .matchedAmount(matchedAmount)
            .unmatchedAmount(unmatchedAmount)
            .discrepancyAmount(unmatchedAmount)
            .matchRate(Math.round(matchRate * 100.0) / 100.0)
            .autoMatchRate(Math.round(autoMatchRate * 100.0) / 100.0)
            .manualMatchRate(100 - autoMatchRate)
            .sourceBreakdowns(sourceBreakdowns)
            .topDiscrepancies(topDiscrepancies)
            .hourlyDistribution(hourlyDist)
            .build();
    }

    /**
     * Generate discrepancy report for date range
     */
    public DiscrepancyReport generateDiscrepancyReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating discrepancy report for {} to {}", startDate, endDate);
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        // Get transactions in range - filtered by current user
        List<Transaction> transactions = getCurrentUserTransactions().stream()
            .filter(t -> t.getTimestamp() != null)
            .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
            .toList();
        
        // Get reconciliations - filtered by current user
        List<Reconciliation> reconciliations = getCurrentUserReconciliations();
        Set<String> matchedRefs = reconciliations.stream()
            .filter(r -> "MATCHED".equals(r.getState()) || "AUTO_MATCHED".equals(r.getState()))
            .map(Reconciliation::getTransactionRef)
            .collect(Collectors.toSet());
        
        // Find discrepancies (unmatched transactions)
        List<DiscrepancyItem> discrepancies = new ArrayList<>();
        
        for (Transaction txn : transactions) {
            if (!matchedRefs.contains(txn.getNormalizedReference())) {
                String discType = determineDiscrepancyType(txn, transactions);
                int priority = calculatePriority(txn.getAmount());
                
                discrepancies.add(DiscrepancyItem.builder()
                    .reference(txn.getExternalReference())
                    .source(txn.getSource())
                    .expectedAmount(txn.getAmount())
                    .actualAmount(BigDecimal.ZERO)
                    .difference(txn.getAmount())
                    .discrepancyType(discType)
                    .transactionDate(txn.getTimestamp())
                    .status("PENDING")
                    .priority(priority)
                    .build());
            }
        }
        
        // Calculate summary
        int amountMismatches = (int) discrepancies.stream()
            .filter(d -> "AMOUNT_MISMATCH".equals(d.getDiscrepancyType()))
            .count();
        int missing = (int) discrepancies.stream()
            .filter(d -> "MISSING".equals(d.getDiscrepancyType()))
            .count();
        int duplicates = (int) discrepancies.stream()
            .filter(d -> "DUPLICATE".equals(d.getDiscrepancyType()))
            .count();
        
        BigDecimal totalDiscrepancyAmount = discrepancies.stream()
            .map(DiscrepancyItem::getDifference)
            .filter(Objects::nonNull)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return DiscrepancyReport.builder()
            .startDate(startDate)
            .endDate(endDate)
            .generatedAt(LocalDateTime.now())
            .totalDiscrepancies(discrepancies.size())
            .resolvedDiscrepancies(0)
            .pendingDiscrepancies(discrepancies.size())
            .totalDiscrepancyAmount(totalDiscrepancyAmount)
            .amountMismatches(amountMismatches)
            .missingTransactions(missing)
            .duplicateTransactions(duplicates)
            .highPriority((int) discrepancies.stream().filter(d -> d.getPriority() == 1).count())
            .mediumPriority((int) discrepancies.stream().filter(d -> d.getPriority() == 2).count())
            .lowPriority((int) discrepancies.stream().filter(d -> d.getPriority() == 3).count())
            .discrepancies(discrepancies)
            .build();
    }

    /**
     * Generate settlement reconciliation report
     */
    public SettlementReport generateSettlementReport(LocalDate settlementDate, String bankName) {
        log.info("Generating settlement report for {} - {}", settlementDate, bankName);
        
        LocalDateTime start = settlementDate.atStartOfDay();
        LocalDateTime end = settlementDate.atTime(LocalTime.MAX);
        
        // Get all transactions for current user
        List<Transaction> allUserTransactions = getCurrentUserTransactions();
        
        // Get bank transactions
        List<Transaction> bankTxns = allUserTransactions.stream()
            .filter(t -> "bank".equals(t.getSource()))
            .filter(t -> t.getTimestamp() != null)
            .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
            .toList();
        
        // Get system transactions (paystack/ledger)
        List<Transaction> systemTxns = allUserTransactions.stream()
            .filter(t -> !"bank".equals(t.getSource()))
            .filter(t -> t.getTimestamp() != null)
            .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
            .toList();
        
        // Create reference map for matching
        Map<String, Transaction> systemRefMap = systemTxns.stream()
            .collect(Collectors.toMap(
                Transaction::getNormalizedReference,
                t -> t,
                (a, b) -> a
            ));
        
        List<SettlementLineItem> lineItems = new ArrayList<>();
        int matchedCount = 0;
        int missingFromBank = 0;
        int missingFromSystem = 0;
        
        // Match bank transactions to system
        for (Transaction bankTxn : bankTxns) {
            Transaction systemTxn = systemRefMap.get(bankTxn.getNormalizedReference());
            
            String status;
            BigDecimal systemAmount;
            BigDecimal fee = BigDecimal.ZERO;
            
            if (systemTxn != null) {
                systemAmount = systemTxn.getAmount();
                BigDecimal diff = bankTxn.getAmount().subtract(systemAmount).abs();
                
                if (diff.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                    status = "MATCHED";
                    matchedCount++;
                } else {
                    status = "AMOUNT_VARIANCE";
                    fee = diff; // Assume difference is fee
                }
                
                systemRefMap.remove(bankTxn.getNormalizedReference());
            } else {
                systemAmount = BigDecimal.ZERO;
                status = "MISSING_FROM_SYSTEM";
                missingFromSystem++;
            }
            
            lineItems.add(SettlementLineItem.builder()
                .reference(bankTxn.getExternalReference())
                .systemAmount(systemAmount)
                .bankAmount(bankTxn.getAmount())
                .fee(fee)
                .netAmount(bankTxn.getAmount())
                .status(status)
                .transactionDate(bankTxn.getTimestamp())
                .settlementDate(settlementDate.atStartOfDay())
                .build());
        }
        
        // Remaining system transactions are missing from bank
        for (Transaction systemTxn : systemRefMap.values()) {
            lineItems.add(SettlementLineItem.builder()
                .reference(systemTxn.getExternalReference())
                .systemAmount(systemTxn.getAmount())
                .bankAmount(BigDecimal.ZERO)
                .fee(BigDecimal.ZERO)
                .netAmount(BigDecimal.ZERO)
                .status("MISSING_FROM_BANK")
                .transactionDate(systemTxn.getTimestamp())
                .settlementDate(settlementDate.atStartOfDay())
                .build());
            missingFromBank++;
        }
        
        // Calculate totals
        BigDecimal expectedSettlement = systemTxns.stream()
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal actualSettlement = bankTxns.stream()
            .map(Transaction::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFees = lineItems.stream()
            .map(SettlementLineItem::getFee)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return SettlementReport.builder()
            .settlementDate(settlementDate)
            .generatedAt(LocalDateTime.now())
            .bankName(bankName)
            .expectedSettlement(expectedSettlement)
            .actualSettlement(actualSettlement)
            .variance(actualSettlement.subtract(expectedSettlement))
            .expectedTransactionCount(systemTxns.size())
            .actualTransactionCount(bankTxns.size())
            .matchedCount(matchedCount)
            .missingFromBank(missingFromBank)
            .missingFromSystem(missingFromSystem)
            .totalFees(totalFees)
            .expectedFees(totalFees)
            .feeVariance(BigDecimal.ZERO)
            .lineItems(lineItems)
            .build();
    }

    /**
     * Generate audit trail report
     */
    public AuditTrailReport generateAuditTrailReport(LocalDate startDate, LocalDate endDate) {
        log.info("Generating audit trail report for {} to {}", startDate, endDate);
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        List<Reconciliation> reconciliations = getCurrentUserReconciliations().stream()
            .filter(r -> r.getCreatedAt() != null)
            .filter(r -> !r.getCreatedAt().isBefore(start) && !r.getCreatedAt().isAfter(end))
            .toList();
        
        List<AuditEntry> entries = new ArrayList<>();
        
        for (Reconciliation recon : reconciliations) {
            if (recon.getAuditTrail() != null) {
                for (Reconciliation.AuditEntry auditEntry : recon.getAuditTrail()) {
                    entries.add(AuditEntry.builder()
                        .timestamp(auditEntry.getTimestamp())
                        .action(auditEntry.getAction())
                        .entityType("RECONCILIATION")
                        .entityId(recon.getId().toString())
                        .reference(recon.getTransactionRef())
                        .user(auditEntry.getActor())
                        .details(auditEntry.getReason())
                        .build());
                }
            }
        }
        
        // Sort by timestamp descending
        entries.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
        
        return AuditTrailReport.builder()
            .startDate(startDate)
            .endDate(endDate)
            .generatedAt(LocalDateTime.now())
            .entries(entries)
            .build();
    }

    // Helper methods
    
    private List<SourceBreakdown> calculateSourceBreakdowns(List<Transaction> transactions, Set<String> matchedRefs) {
        Map<String, List<Transaction>> bySource = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::getSource));
        
        return bySource.entrySet().stream()
            .map(entry -> {
                String source = entry.getKey();
                List<Transaction> txns = entry.getValue();
                int total = txns.size();
                int matched = (int) txns.stream()
                    .filter(t -> matchedRefs.contains(t.getNormalizedReference()))
                    .count();
                BigDecimal totalAmount = txns.stream()
                    .map(Transaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return SourceBreakdown.builder()
                    .source(source)
                    .transactionCount(total)
                    .totalAmount(totalAmount)
                    .matchedCount(matched)
                    .unmatchedCount(total - matched)
                    .matchRate(total > 0 ? Math.round((double) matched / total * 10000.0) / 100.0 : 0)
                    .build();
            })
            .toList();
    }
    
    private List<DiscrepancyItem> findTopDiscrepancies(List<Transaction> transactions, 
            List<Reconciliation> reconciliations, int limit) {
        Set<String> matchedRefs = reconciliations.stream()
            .filter(r -> "MATCHED".equals(r.getState()) || "AUTO_MATCHED".equals(r.getState()))
            .map(Reconciliation::getTransactionRef)
            .collect(Collectors.toSet());
        
        return transactions.stream()
            .filter(t -> !matchedRefs.contains(t.getNormalizedReference()))
            .filter(t -> t.getAmount() != null)
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .limit(limit)
            .map(t -> DiscrepancyItem.builder()
                .reference(t.getExternalReference())
                .source(t.getSource())
                .expectedAmount(t.getAmount())
                .actualAmount(BigDecimal.ZERO)
                .difference(t.getAmount())
                .discrepancyType("MISSING")
                .transactionDate(t.getTimestamp())
                .status("PENDING")
                .priority(calculatePriority(t.getAmount()))
                .build())
            .toList();
    }
    
    private String determineDiscrepancyType(Transaction txn, List<Transaction> allTxns) {
        // Check for duplicates
        long sameAmountCount = allTxns.stream()
            .filter(t -> t.getAmount() != null && txn.getAmount() != null)
            .filter(t -> t.getAmount().compareTo(txn.getAmount()) == 0)
            .filter(t -> !t.getId().equals(txn.getId()))
            .count();
        
        if (sameAmountCount > 0) {
            return "DUPLICATE";
        }
        
        return "MISSING";
    }
    
    private int calculatePriority(BigDecimal amount) {
        if (amount == null) return 3;
        
        if (amount.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            return 1; // HIGH
        } else if (amount.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            return 2; // MEDIUM
        } else {
            return 3; // LOW
        }
    }
}
