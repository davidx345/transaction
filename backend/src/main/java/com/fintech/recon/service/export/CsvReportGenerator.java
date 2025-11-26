package com.fintech.recon.service.export;

import com.fintech.recon.dto.ReportDtos.*;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates CSV reports using OpenCSV
 */
@Component
public class CsvReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate daily summary report as CSV
     */
    public byte[] generateDailySummaryCsv(DailySummaryReport report) throws IOException {
        List<String[]> lines = new ArrayList<>();
        
        // Header info
        lines.add(new String[]{"Daily Reconciliation Summary"});
        lines.add(new String[]{"Report Date", report.getReportDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"Generated At", report.getGeneratedAt().format(DATETIME_FORMATTER)});
        lines.add(new String[]{}); // Empty row
        
        // Summary section
        lines.add(new String[]{"Summary"});
        lines.add(new String[]{"Metric", "Value"});
        lines.add(new String[]{"Total Transactions", String.valueOf(report.getTotalTransactions())});
        lines.add(new String[]{"Matched Transactions", String.valueOf(report.getMatchedTransactions())});
        lines.add(new String[]{"Unmatched Transactions", String.valueOf(report.getUnmatchedTransactions())});
        lines.add(new String[]{"Pending Transactions", String.valueOf(report.getPendingTransactions())});
        lines.add(new String[]{"Disputed Transactions", String.valueOf(report.getDisputedTransactions())});
        lines.add(new String[]{}); // Empty row
        
        // Amounts section
        lines.add(new String[]{"Amounts"});
        lines.add(new String[]{"Total Amount", formatAmount(report.getTotalAmount())});
        lines.add(new String[]{"Matched Amount", formatAmount(report.getMatchedAmount())});
        lines.add(new String[]{"Unmatched Amount", formatAmount(report.getUnmatchedAmount())});
        lines.add(new String[]{"Discrepancy Amount", formatAmount(report.getDiscrepancyAmount())});
        lines.add(new String[]{}); // Empty row
        
        // Match rates
        lines.add(new String[]{"Match Rates"});
        lines.add(new String[]{"Overall Match Rate", formatPercent(report.getMatchRate())});
        lines.add(new String[]{"Auto Match Rate", formatPercent(report.getAutoMatchRate())});
        lines.add(new String[]{"Manual Match Rate", formatPercent(report.getManualMatchRate())});
        lines.add(new String[]{}); // Empty row
        
        // Source breakdown
        if (report.getSourceBreakdowns() != null && !report.getSourceBreakdowns().isEmpty()) {
            lines.add(new String[]{"Source Breakdown"});
            lines.add(new String[]{"Source", "Count", "Amount", "Matched", "Unmatched", "Match Rate"});
            for (SourceBreakdown sb : report.getSourceBreakdowns()) {
                lines.add(new String[]{
                    sb.getSource(),
                    String.valueOf(sb.getTransactionCount()),
                    formatAmount(sb.getTotalAmount()),
                    String.valueOf(sb.getMatchedCount()),
                    String.valueOf(sb.getUnmatchedCount()),
                    formatPercent(sb.getMatchRate())
                });
            }
        }
        
        return writeCsv(lines);
    }

    /**
     * Generate discrepancy report as CSV
     */
    public byte[] generateDiscrepancyReportCsv(DiscrepancyReport report) throws IOException {
        List<String[]> lines = new ArrayList<>();
        
        // Header info
        lines.add(new String[]{"Discrepancy Report"});
        lines.add(new String[]{"Start Date", report.getStartDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"End Date", report.getEndDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"Generated At", report.getGeneratedAt().format(DATETIME_FORMATTER)});
        lines.add(new String[]{}); // Empty row
        
        // Summary
        lines.add(new String[]{"Summary"});
        lines.add(new String[]{"Total Discrepancies", String.valueOf(report.getTotalDiscrepancies())});
        lines.add(new String[]{"Pending", String.valueOf(report.getPendingDiscrepancies())});
        lines.add(new String[]{"Resolved", String.valueOf(report.getResolvedDiscrepancies())});
        lines.add(new String[]{"Total Amount", formatAmount(report.getTotalDiscrepancyAmount())});
        lines.add(new String[]{}); // Empty row
        
        // By type
        lines.add(new String[]{"By Type"});
        lines.add(new String[]{"Amount Mismatches", String.valueOf(report.getAmountMismatches())});
        lines.add(new String[]{"Missing Transactions", String.valueOf(report.getMissingTransactions())});
        lines.add(new String[]{"Duplicates", String.valueOf(report.getDuplicateTransactions())});
        lines.add(new String[]{}); // Empty row
        
        // By priority
        lines.add(new String[]{"By Priority"});
        lines.add(new String[]{"High", String.valueOf(report.getHighPriority())});
        lines.add(new String[]{"Medium", String.valueOf(report.getMediumPriority())});
        lines.add(new String[]{"Low", String.valueOf(report.getLowPriority())});
        lines.add(new String[]{}); // Empty row
        
        // Detail section
        lines.add(new String[]{"Discrepancy Details"});
        lines.add(new String[]{"Reference", "Source", "Expected", "Actual", "Difference", "Type", "Date", "Status", "Priority"});
        
        if (report.getDiscrepancies() != null) {
            for (DiscrepancyItem item : report.getDiscrepancies()) {
                lines.add(new String[]{
                    item.getReference() != null ? item.getReference() : "",
                    item.getSource() != null ? item.getSource() : "",
                    formatAmount(item.getExpectedAmount()),
                    formatAmount(item.getActualAmount()),
                    formatAmount(item.getDifference()),
                    item.getDiscrepancyType() != null ? item.getDiscrepancyType() : "",
                    item.getTransactionDate() != null ? item.getTransactionDate().format(DATETIME_FORMATTER) : "",
                    item.getStatus() != null ? item.getStatus() : "",
                    getPriorityLabel(item.getPriority())
                });
            }
        }
        
        return writeCsv(lines);
    }

    /**
     * Generate settlement report as CSV
     */
    public byte[] generateSettlementReportCsv(SettlementReport report) throws IOException {
        List<String[]> lines = new ArrayList<>();
        
        // Header info
        lines.add(new String[]{"Settlement Report"});
        lines.add(new String[]{"Settlement Date", report.getSettlementDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"Bank", report.getBankName()});
        lines.add(new String[]{"Generated At", report.getGeneratedAt().format(DATETIME_FORMATTER)});
        lines.add(new String[]{}); // Empty row
        
        // Settlement comparison
        lines.add(new String[]{"Settlement Comparison"});
        lines.add(new String[]{"Expected Settlement", formatAmount(report.getExpectedSettlement())});
        lines.add(new String[]{"Actual Settlement", formatAmount(report.getActualSettlement())});
        lines.add(new String[]{"Variance", formatAmount(report.getVariance())});
        lines.add(new String[]{}); // Empty row
        
        // Transaction comparison
        lines.add(new String[]{"Transaction Comparison"});
        lines.add(new String[]{"Expected Count", String.valueOf(report.getExpectedTransactionCount())});
        lines.add(new String[]{"Actual Count", String.valueOf(report.getActualTransactionCount())});
        lines.add(new String[]{"Matched", String.valueOf(report.getMatchedCount())});
        lines.add(new String[]{"Missing from Bank", String.valueOf(report.getMissingFromBank())});
        lines.add(new String[]{"Missing from System", String.valueOf(report.getMissingFromSystem())});
        lines.add(new String[]{}); // Empty row
        
        // Fee analysis
        lines.add(new String[]{"Fee Analysis"});
        lines.add(new String[]{"Total Fees", formatAmount(report.getTotalFees())});
        lines.add(new String[]{"Expected Fees", formatAmount(report.getExpectedFees())});
        lines.add(new String[]{"Fee Variance", formatAmount(report.getFeeVariance())});
        lines.add(new String[]{}); // Empty row
        
        // Detail section
        lines.add(new String[]{"Line Items"});
        lines.add(new String[]{"Reference", "System Amount", "Bank Amount", "Fee", "Net Amount", "Status", "Transaction Date", "Settlement Date"});
        
        if (report.getLineItems() != null) {
            for (SettlementLineItem item : report.getLineItems()) {
                lines.add(new String[]{
                    item.getReference() != null ? item.getReference() : "",
                    formatAmount(item.getSystemAmount()),
                    formatAmount(item.getBankAmount()),
                    formatAmount(item.getFee()),
                    formatAmount(item.getNetAmount()),
                    item.getStatus() != null ? item.getStatus() : "",
                    item.getTransactionDate() != null ? item.getTransactionDate().format(DATETIME_FORMATTER) : "",
                    item.getSettlementDate() != null ? item.getSettlementDate().format(DATETIME_FORMATTER) : ""
                });
            }
        }
        
        return writeCsv(lines);
    }

    /**
     * Generate audit trail report as CSV
     */
    public byte[] generateAuditTrailCsv(AuditTrailReport report) throws IOException {
        List<String[]> lines = new ArrayList<>();
        
        // Header info
        lines.add(new String[]{"Audit Trail Report"});
        lines.add(new String[]{"Start Date", report.getStartDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"End Date", report.getEndDate().format(DATE_FORMATTER)});
        lines.add(new String[]{"Generated At", report.getGeneratedAt().format(DATETIME_FORMATTER)});
        lines.add(new String[]{}); // Empty row
        
        // Detail section
        lines.add(new String[]{"Timestamp", "Action", "Entity Type", "Entity ID", "Reference", "User", "Details"});
        
        if (report.getEntries() != null) {
            for (AuditEntry entry : report.getEntries()) {
                lines.add(new String[]{
                    entry.getTimestamp() != null ? entry.getTimestamp().format(DATETIME_FORMATTER) : "",
                    entry.getAction() != null ? entry.getAction() : "",
                    entry.getEntityType() != null ? entry.getEntityType() : "",
                    entry.getEntityId() != null ? entry.getEntityId() : "",
                    entry.getReference() != null ? entry.getReference() : "",
                    entry.getUser() != null ? entry.getUser() : "",
                    entry.getDetails() != null ? entry.getDetails() : ""
                });
            }
        }
        
        return writeCsv(lines);
    }

    /**
     * Generate generic transaction export CSV
     */
    public byte[] generateTransactionExportCsv(List<TransactionExportRow> transactions) throws IOException {
        List<String[]> lines = new ArrayList<>();
        
        // Header
        lines.add(new String[]{
            "Reference", "External Reference", "Source", "Amount", "Currency", 
            "Status", "Timestamp", "Counterparty", "Description", "Match Status"
        });
        
        for (TransactionExportRow txn : transactions) {
            lines.add(new String[]{
                txn.getReference() != null ? txn.getReference() : "",
                txn.getExternalReference() != null ? txn.getExternalReference() : "",
                txn.getSource() != null ? txn.getSource() : "",
                formatAmount(txn.getAmount()),
                txn.getCurrency() != null ? txn.getCurrency() : "NGN",
                txn.getStatus() != null ? txn.getStatus() : "",
                txn.getTimestamp() != null ? txn.getTimestamp().format(DATETIME_FORMATTER) : "",
                txn.getCounterparty() != null ? txn.getCounterparty() : "",
                txn.getDescription() != null ? txn.getDescription() : "",
                txn.getMatchStatus() != null ? txn.getMatchStatus() : ""
            });
        }
        
        return writeCsv(lines);
    }

    // Helper methods
    
    private byte[] writeCsv(List<String[]> lines) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            
            csvWriter.writeAll(lines);
            csvWriter.flush();
            
            return out.toByteArray();
        }
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%.2f", amount);
    }
    
    private String formatPercent(double percent) {
        return String.format("%.2f%%", percent);
    }
    
    private String getPriorityLabel(int priority) {
        return switch (priority) {
            case 1 -> "HIGH";
            case 2 -> "MEDIUM";
            default -> "LOW";
        };
    }

    /**
     * DTO for transaction export
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionExportRow {
        private String reference;
        private String externalReference;
        private String source;
        private BigDecimal amount;
        private String currency;
        private String status;
        private java.time.LocalDateTime timestamp;
        private String counterparty;
        private String description;
        private String matchStatus;
    }
}
