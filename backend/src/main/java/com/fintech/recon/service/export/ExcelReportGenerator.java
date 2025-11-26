package com.fintech.recon.service.export;

import com.fintech.recon.dto.ReportDtos.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates Excel reports using Apache POI
 */
@Component
public class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate daily summary report as Excel
     */
    public byte[] generateDailySummaryExcel(DailySummaryReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Summary");
            
            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            
            int rowNum = 0;
            
            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Daily Reconciliation Summary - " + report.getReportDate().format(DATE_FORMATTER));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            // Generated at
            Row genRow = sheet.createRow(rowNum++);
            genRow.createCell(0).setCellValue("Generated: " + report.getGeneratedAt().format(DATETIME_FORMATTER));
            rowNum++; // Empty row
            
            // Summary section
            Row summaryHeader = sheet.createRow(rowNum++);
            summaryHeader.createCell(0).setCellValue("Summary");
            summaryHeader.getCell(0).setCellStyle(headerStyle);
            
            createSummaryRow(sheet, rowNum++, "Total Transactions", String.valueOf(report.getTotalTransactions()));
            createSummaryRow(sheet, rowNum++, "Matched Transactions", String.valueOf(report.getMatchedTransactions()));
            createSummaryRow(sheet, rowNum++, "Unmatched Transactions", String.valueOf(report.getUnmatchedTransactions()));
            createSummaryRow(sheet, rowNum++, "Pending Transactions", String.valueOf(report.getPendingTransactions()));
            createSummaryRow(sheet, rowNum++, "Disputed Transactions", String.valueOf(report.getDisputedTransactions()));
            rowNum++; // Empty row
            
            // Amounts section
            Row amountsHeader = sheet.createRow(rowNum++);
            amountsHeader.createCell(0).setCellValue("Amounts");
            amountsHeader.getCell(0).setCellStyle(headerStyle);
            
            createAmountRow(sheet, rowNum++, "Total Amount", report.getTotalAmount(), currencyStyle);
            createAmountRow(sheet, rowNum++, "Matched Amount", report.getMatchedAmount(), currencyStyle);
            createAmountRow(sheet, rowNum++, "Unmatched Amount", report.getUnmatchedAmount(), currencyStyle);
            createAmountRow(sheet, rowNum++, "Discrepancy Amount", report.getDiscrepancyAmount(), currencyStyle);
            rowNum++; // Empty row
            
            // Match rates section
            Row ratesHeader = sheet.createRow(rowNum++);
            ratesHeader.createCell(0).setCellValue("Match Rates");
            ratesHeader.getCell(0).setCellStyle(headerStyle);
            
            createPercentRow(sheet, rowNum++, "Overall Match Rate", report.getMatchRate(), percentStyle);
            createPercentRow(sheet, rowNum++, "Auto Match Rate", report.getAutoMatchRate(), percentStyle);
            createPercentRow(sheet, rowNum++, "Manual Match Rate", report.getManualMatchRate(), percentStyle);
            rowNum++; // Empty row
            
            // Source breakdown section
            if (report.getSourceBreakdowns() != null && !report.getSourceBreakdowns().isEmpty()) {
                Row sourceHeader = sheet.createRow(rowNum++);
                sourceHeader.createCell(0).setCellValue("Source Breakdown");
                sourceHeader.getCell(0).setCellStyle(headerStyle);
                
                Row sourceColHeaders = sheet.createRow(rowNum++);
                String[] sourceHeaders = {"Source", "Count", "Amount", "Matched", "Unmatched", "Match Rate"};
                for (int i = 0; i < sourceHeaders.length; i++) {
                    Cell cell = sourceColHeaders.createCell(i);
                    cell.setCellValue(sourceHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (SourceBreakdown sb : report.getSourceBreakdowns()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(sb.getSource());
                    row.createCell(1).setCellValue(sb.getTransactionCount());
                    createCurrencyCell(row, 2, sb.getTotalAmount(), currencyStyle);
                    row.createCell(3).setCellValue(sb.getMatchedCount());
                    row.createCell(4).setCellValue(sb.getUnmatchedCount());
                    createPercentCell(row, 5, sb.getMatchRate(), percentStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            return toByteArray(workbook);
        }
    }

    /**
     * Generate discrepancy report as Excel
     */
    public byte[] generateDiscrepancyReportExcel(DiscrepancyReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summarySheet = workbook.createSheet("Summary");
            Sheet detailSheet = workbook.createSheet("Discrepancies");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            
            // Summary sheet
            int rowNum = 0;
            
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Discrepancy Report: " + report.getStartDate().format(DATE_FORMATTER) + 
                " to " + report.getEndDate().format(DATE_FORMATTER));
            titleCell.setCellStyle(titleStyle);
            
            Row genRow = summarySheet.createRow(rowNum++);
            genRow.createCell(0).setCellValue("Generated: " + report.getGeneratedAt().format(DATETIME_FORMATTER));
            rowNum++;
            
            createSummaryRow(summarySheet, rowNum++, "Total Discrepancies", String.valueOf(report.getTotalDiscrepancies()));
            createSummaryRow(summarySheet, rowNum++, "Pending", String.valueOf(report.getPendingDiscrepancies()));
            createSummaryRow(summarySheet, rowNum++, "Resolved", String.valueOf(report.getResolvedDiscrepancies()));
            createAmountRow(summarySheet, rowNum++, "Total Amount", report.getTotalDiscrepancyAmount(), currencyStyle);
            rowNum++;
            
            Row typeHeader = summarySheet.createRow(rowNum++);
            typeHeader.createCell(0).setCellValue("By Type");
            typeHeader.getCell(0).setCellStyle(headerStyle);
            
            createSummaryRow(summarySheet, rowNum++, "Amount Mismatches", String.valueOf(report.getAmountMismatches()));
            createSummaryRow(summarySheet, rowNum++, "Missing Transactions", String.valueOf(report.getMissingTransactions()));
            createSummaryRow(summarySheet, rowNum++, "Duplicates", String.valueOf(report.getDuplicateTransactions()));
            rowNum++;
            
            Row priorityHeader = summarySheet.createRow(rowNum++);
            priorityHeader.createCell(0).setCellValue("By Priority");
            priorityHeader.getCell(0).setCellStyle(headerStyle);
            
            createSummaryRow(summarySheet, rowNum++, "High Priority", String.valueOf(report.getHighPriority()));
            createSummaryRow(summarySheet, rowNum++, "Medium Priority", String.valueOf(report.getMediumPriority()));
            createSummaryRow(summarySheet, rowNum++, "Low Priority", String.valueOf(report.getLowPriority()));
            
            for (int i = 0; i < 4; i++) {
                summarySheet.autoSizeColumn(i);
            }
            
            // Detail sheet
            rowNum = 0;
            Row detailHeader = detailSheet.createRow(rowNum++);
            String[] headers = {"Reference", "Source", "Expected", "Actual", "Difference", "Type", "Date", "Status", "Priority"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = detailHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            if (report.getDiscrepancies() != null) {
                for (DiscrepancyItem item : report.getDiscrepancies()) {
                    Row row = detailSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.getReference() != null ? item.getReference() : "");
                    row.createCell(1).setCellValue(item.getSource() != null ? item.getSource() : "");
                    createCurrencyCell(row, 2, item.getExpectedAmount(), currencyStyle);
                    createCurrencyCell(row, 3, item.getActualAmount(), currencyStyle);
                    createCurrencyCell(row, 4, item.getDifference(), currencyStyle);
                    row.createCell(5).setCellValue(item.getDiscrepancyType() != null ? item.getDiscrepancyType() : "");
                    row.createCell(6).setCellValue(item.getTransactionDate() != null ? 
                        item.getTransactionDate().format(DATETIME_FORMATTER) : "");
                    row.createCell(7).setCellValue(item.getStatus() != null ? item.getStatus() : "");
                    row.createCell(8).setCellValue(getPriorityLabel(item.getPriority()));
                }
            }
            
            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }
            
            return toByteArray(workbook);
        }
    }

    /**
     * Generate settlement report as Excel
     */
    public byte[] generateSettlementReportExcel(SettlementReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summarySheet = workbook.createSheet("Summary");
            Sheet detailSheet = workbook.createSheet("Line Items");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            
            // Summary sheet
            int rowNum = 0;
            
            Row titleRow = summarySheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Settlement Report - " + report.getSettlementDate().format(DATE_FORMATTER));
            titleCell.setCellStyle(titleStyle);
            
            Row bankRow = summarySheet.createRow(rowNum++);
            bankRow.createCell(0).setCellValue("Bank: " + report.getBankName());
            
            Row genRow = summarySheet.createRow(rowNum++);
            genRow.createCell(0).setCellValue("Generated: " + report.getGeneratedAt().format(DATETIME_FORMATTER));
            rowNum++;
            
            Row settlementHeader = summarySheet.createRow(rowNum++);
            settlementHeader.createCell(0).setCellValue("Settlement Comparison");
            settlementHeader.getCell(0).setCellStyle(headerStyle);
            
            createAmountRow(summarySheet, rowNum++, "Expected Settlement", report.getExpectedSettlement(), currencyStyle);
            createAmountRow(summarySheet, rowNum++, "Actual Settlement", report.getActualSettlement(), currencyStyle);
            createAmountRow(summarySheet, rowNum++, "Variance", report.getVariance(), currencyStyle);
            rowNum++;
            
            Row txnHeader = summarySheet.createRow(rowNum++);
            txnHeader.createCell(0).setCellValue("Transaction Comparison");
            txnHeader.getCell(0).setCellStyle(headerStyle);
            
            createSummaryRow(summarySheet, rowNum++, "Expected Count", String.valueOf(report.getExpectedTransactionCount()));
            createSummaryRow(summarySheet, rowNum++, "Actual Count", String.valueOf(report.getActualTransactionCount()));
            createSummaryRow(summarySheet, rowNum++, "Matched", String.valueOf(report.getMatchedCount()));
            createSummaryRow(summarySheet, rowNum++, "Missing from Bank", String.valueOf(report.getMissingFromBank()));
            createSummaryRow(summarySheet, rowNum++, "Missing from System", String.valueOf(report.getMissingFromSystem()));
            rowNum++;
            
            Row feeHeader = summarySheet.createRow(rowNum++);
            feeHeader.createCell(0).setCellValue("Fee Analysis");
            feeHeader.getCell(0).setCellStyle(headerStyle);
            
            createAmountRow(summarySheet, rowNum++, "Total Fees", report.getTotalFees(), currencyStyle);
            createAmountRow(summarySheet, rowNum++, "Expected Fees", report.getExpectedFees(), currencyStyle);
            createAmountRow(summarySheet, rowNum++, "Fee Variance", report.getFeeVariance(), currencyStyle);
            
            for (int i = 0; i < 4; i++) {
                summarySheet.autoSizeColumn(i);
            }
            
            // Detail sheet
            rowNum = 0;
            Row detailHeader = detailSheet.createRow(rowNum++);
            String[] headers = {"Reference", "System Amount", "Bank Amount", "Fee", "Net Amount", "Status", "Transaction Date", "Settlement Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = detailHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            if (report.getLineItems() != null) {
                for (SettlementLineItem item : report.getLineItems()) {
                    Row row = detailSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.getReference() != null ? item.getReference() : "");
                    createCurrencyCell(row, 1, item.getSystemAmount(), currencyStyle);
                    createCurrencyCell(row, 2, item.getBankAmount(), currencyStyle);
                    createCurrencyCell(row, 3, item.getFee(), currencyStyle);
                    createCurrencyCell(row, 4, item.getNetAmount(), currencyStyle);
                    row.createCell(5).setCellValue(item.getStatus() != null ? item.getStatus() : "");
                    row.createCell(6).setCellValue(item.getTransactionDate() != null ? 
                        item.getTransactionDate().format(DATETIME_FORMATTER) : "");
                    row.createCell(7).setCellValue(item.getSettlementDate() != null ? 
                        item.getSettlementDate().format(DATETIME_FORMATTER) : "");
                }
            }
            
            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }
            
            return toByteArray(workbook);
        }
    }

    /**
     * Generate audit trail report as Excel
     */
    public byte[] generateAuditTrailExcel(AuditTrailReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Trail");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Audit Trail Report: " + report.getStartDate().format(DATE_FORMATTER) + 
                " to " + report.getEndDate().format(DATE_FORMATTER));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            
            Row genRow = sheet.createRow(rowNum++);
            genRow.createCell(0).setCellValue("Generated: " + report.getGeneratedAt().format(DATETIME_FORMATTER));
            rowNum++;
            
            Row header = sheet.createRow(rowNum++);
            String[] headers = {"Timestamp", "Action", "Entity Type", "Reference", "User", "Details"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            if (report.getEntries() != null) {
                for (AuditEntry entry : report.getEntries()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getTimestamp() != null ? 
                        entry.getTimestamp().format(DATETIME_FORMATTER) : "");
                    row.createCell(1).setCellValue(entry.getAction() != null ? entry.getAction() : "");
                    row.createCell(2).setCellValue(entry.getEntityType() != null ? entry.getEntityType() : "");
                    row.createCell(3).setCellValue(entry.getReference() != null ? entry.getReference() : "");
                    row.createCell(4).setCellValue(entry.getUser() != null ? entry.getUser() : "");
                    row.createCell(5).setCellValue(entry.getDetails() != null ? entry.getDetails() : "");
                }
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            return toByteArray(workbook);
        }
    }

    // Helper methods
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }
    
    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }
    
    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
    
    private void createAmountRow(Sheet sheet, int rowNum, String label, BigDecimal amount, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        if (amount != null) {
            valueCell.setCellValue(amount.doubleValue());
            valueCell.setCellStyle(style);
        } else {
            valueCell.setCellValue(0.0);
        }
    }
    
    private void createPercentRow(Sheet sheet, int rowNum, String label, double percent, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(percent / 100.0);
        valueCell.setCellStyle(style);
    }
    
    private void createCurrencyCell(Row row, int col, BigDecimal amount, CellStyle style) {
        Cell cell = row.createCell(col);
        if (amount != null) {
            cell.setCellValue(amount.doubleValue());
            cell.setCellStyle(style);
        } else {
            cell.setCellValue(0.0);
        }
    }
    
    private void createPercentCell(Row row, int col, double percent, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(percent / 100.0);
        cell.setCellStyle(style);
    }
    
    private String getPriorityLabel(int priority) {
        return switch (priority) {
            case 1 -> "HIGH";
            case 2 -> "MEDIUM";
            default -> "LOW";
        };
    }
    
    private byte[] toByteArray(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
