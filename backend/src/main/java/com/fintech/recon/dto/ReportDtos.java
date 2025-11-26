package com.fintech.recon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTOs for reconciliation reports
 */
public class ReportDtos {

    /**
     * Request for generating a report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private ReportType reportType;
        private LocalDate startDate;
        private LocalDate endDate;
        private ExportFormat exportFormat;
        private List<String> sources; // Filter by sources
        private List<String> statuses; // Filter by statuses
        private boolean includeDetails;
    }

    /**
     * Types of reports available
     */
    public enum ReportType {
        DAILY_SUMMARY,
        DISCREPANCY_REPORT,
        AUDIT_TRAIL,
        DISPUTE_REPORT,
        TRANSACTION_DETAIL,
        SETTLEMENT_RECONCILIATION
    }

    /**
     * Export formats
     */
    public enum ExportFormat {
        JSON,
        CSV,
        EXCEL,
        PDF
    }

    /**
     * Daily reconciliation summary report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummaryReport {
        private LocalDate reportDate;
        private LocalDateTime generatedAt;
        
        // Transaction counts
        private int totalTransactions;
        private int matchedTransactions;
        private int unmatchedTransactions;
        private int pendingTransactions;
        private int disputedTransactions;
        
        // Amounts
        private BigDecimal totalAmount;
        private BigDecimal matchedAmount;
        private BigDecimal unmatchedAmount;
        private BigDecimal discrepancyAmount;
        
        // Match rates
        private double matchRate;
        private double autoMatchRate;
        private double manualMatchRate;
        
        // Breakdown by source
        @Builder.Default
        private List<SourceBreakdown> sourceBreakdowns = new ArrayList<>();
        
        // Top discrepancies
        @Builder.Default
        private List<DiscrepancyItem> topDiscrepancies = new ArrayList<>();
        
        // Hourly distribution
        @Builder.Default
        private Map<Integer, Integer> hourlyDistribution = Map.of();
    }

    /**
     * Breakdown by transaction source
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceBreakdown {
        private String source;
        private int transactionCount;
        private BigDecimal totalAmount;
        private int matchedCount;
        private int unmatchedCount;
        private double matchRate;
    }

    /**
     * Discrepancy item for reports
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscrepancyItem {
        private String reference;
        private String source;
        private String counterpartySource;
        private BigDecimal expectedAmount;
        private BigDecimal actualAmount;
        private BigDecimal difference;
        private String discrepancyType; // AMOUNT_MISMATCH, MISSING, DUPLICATE
        private LocalDateTime transactionDate;
        private String status;
        private int priority; // 1=HIGH, 2=MEDIUM, 3=LOW
    }

    /**
     * Full discrepancy report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscrepancyReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        
        // Summary
        private int totalDiscrepancies;
        private int resolvedDiscrepancies;
        private int pendingDiscrepancies;
        private BigDecimal totalDiscrepancyAmount;
        
        // By type
        private int amountMismatches;
        private int missingTransactions;
        private int duplicateTransactions;
        
        // By priority
        private int highPriority;
        private int mediumPriority;
        private int lowPriority;
        
        // Details
        @Builder.Default
        private List<DiscrepancyItem> discrepancies = new ArrayList<>();
    }

    /**
     * Audit trail report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditTrailReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        
        @Builder.Default
        private List<AuditEntry> entries = new ArrayList<>();
    }

    /**
     * Single audit entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditEntry {
        private LocalDateTime timestamp;
        private String action; // INGESTED, MATCHED, DISPUTED, RESOLVED, EXPORTED
        private String entityType; // TRANSACTION, DISPUTE, RECONCILIATION
        private String entityId;
        private String reference;
        private String user;
        private String details;
        private Map<String, Object> metadata;
    }

    /**
     * Dispute report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisputeReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        
        // Summary
        private int totalDisputes;
        private int openDisputes;
        private int resolvedDisputes;
        private int escalatedDisputes;
        private BigDecimal totalDisputedAmount;
        
        // Resolution stats
        private double avgResolutionTimeHours;
        private int autoResolvedCount;
        private int manualResolvedCount;
        
        // Details
        @Builder.Default
        private List<DisputeItem> disputes = new ArrayList<>();
    }

    /**
     * Dispute item for report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisputeItem {
        private String disputeId;
        private String transactionReference;
        private BigDecimal amount;
        private String reason;
        private String status;
        private String priority;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String resolution;
        private String assignedTo;
    }

    /**
     * Settlement reconciliation report
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementReport {
        private LocalDate settlementDate;
        private LocalDateTime generatedAt;
        private String bankName;
        
        // Expected vs Actual
        private BigDecimal expectedSettlement;
        private BigDecimal actualSettlement;
        private BigDecimal variance;
        
        // Transaction breakdown
        private int expectedTransactionCount;
        private int actualTransactionCount;
        private int matchedCount;
        private int missingFromBank;
        private int missingFromSystem;
        
        // Fee analysis
        private BigDecimal totalFees;
        private BigDecimal expectedFees;
        private BigDecimal feeVariance;
        
        // Details
        @Builder.Default
        private List<SettlementLineItem> lineItems = new ArrayList<>();
    }

    /**
     * Settlement line item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementLineItem {
        private String reference;
        private BigDecimal systemAmount;
        private BigDecimal bankAmount;
        private BigDecimal fee;
        private BigDecimal netAmount;
        private String status; // MATCHED, MISSING_FROM_BANK, MISSING_FROM_SYSTEM, AMOUNT_VARIANCE
        private LocalDateTime transactionDate;
        private LocalDateTime settlementDate;
    }
}
