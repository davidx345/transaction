package com.fintech.recon.api;

import com.fintech.recon.dto.ReportDtos.*;
import com.fintech.recon.service.ReportService;
import com.fintech.recon.service.export.CsvReportGenerator;
import com.fintech.recon.service.export.ExcelReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for generating and exporting reconciliation reports
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final ExcelReportGenerator excelReportGenerator;
    private final CsvReportGenerator csvReportGenerator;

    /**
     * Generate daily summary report
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<DailySummaryReport> getDailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate reportDate = date != null ? date : LocalDate.now();
        log.info("Generating daily summary report for {}", reportDate);
        
        DailySummaryReport report = reportService.generateDailySummary(reportDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate discrepancy report
     */
    @GetMapping("/discrepancies")
    public ResponseEntity<DiscrepancyReport> getDiscrepancyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating discrepancy report for {} to {}", startDate, endDate);
        
        DiscrepancyReport report = reportService.generateDiscrepancyReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate settlement report
     */
    @GetMapping("/settlement")
    public ResponseEntity<SettlementReport> getSettlementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            @RequestParam(defaultValue = "GTBank") String bankName) {
        
        log.info("Generating settlement report for {} - {}", settlementDate, bankName);
        
        SettlementReport report = reportService.generateSettlementReport(settlementDate, bankName);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate audit trail report
     */
    @GetMapping("/audit-trail")
    public ResponseEntity<AuditTrailReport> getAuditTrailReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Generating audit trail report for {} to {}", startDate, endDate);
        
        AuditTrailReport report = reportService.generateAuditTrailReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // ============== Export Endpoints ==============

    /**
     * Export daily summary as Excel
     */
    @GetMapping("/daily-summary/export/excel")
    public ResponseEntity<byte[]> exportDailySummaryExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws IOException {
        
        LocalDate reportDate = date != null ? date : LocalDate.now();
        log.info("Exporting daily summary report as Excel for {}", reportDate);
        
        DailySummaryReport report = reportService.generateDailySummary(reportDate);
        byte[] excelData = excelReportGenerator.generateDailySummaryExcel(report);
        
        String filename = "daily-summary-" + reportDate.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        return createFileResponse(excelData, filename, 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Export daily summary as CSV
     */
    @GetMapping("/daily-summary/export/csv")
    public ResponseEntity<byte[]> exportDailySummaryCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws IOException {
        
        LocalDate reportDate = date != null ? date : LocalDate.now();
        log.info("Exporting daily summary report as CSV for {}", reportDate);
        
        DailySummaryReport report = reportService.generateDailySummary(reportDate);
        byte[] csvData = csvReportGenerator.generateDailySummaryCsv(report);
        
        String filename = "daily-summary-" + reportDate.format(DateTimeFormatter.ISO_DATE) + ".csv";
        return createFileResponse(csvData, filename, "text/csv");
    }

    /**
     * Export discrepancy report as Excel
     */
    @GetMapping("/discrepancies/export/excel")
    public ResponseEntity<byte[]> exportDiscrepancyReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        log.info("Exporting discrepancy report as Excel for {} to {}", startDate, endDate);
        
        DiscrepancyReport report = reportService.generateDiscrepancyReport(startDate, endDate);
        byte[] excelData = excelReportGenerator.generateDiscrepancyReportExcel(report);
        
        String filename = "discrepancy-report-" + startDate.format(DateTimeFormatter.ISO_DATE) + 
            "-to-" + endDate.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        return createFileResponse(excelData, filename, 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Export discrepancy report as CSV
     */
    @GetMapping("/discrepancies/export/csv")
    public ResponseEntity<byte[]> exportDiscrepancyReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        log.info("Exporting discrepancy report as CSV for {} to {}", startDate, endDate);
        
        DiscrepancyReport report = reportService.generateDiscrepancyReport(startDate, endDate);
        byte[] csvData = csvReportGenerator.generateDiscrepancyReportCsv(report);
        
        String filename = "discrepancy-report-" + startDate.format(DateTimeFormatter.ISO_DATE) + 
            "-to-" + endDate.format(DateTimeFormatter.ISO_DATE) + ".csv";
        return createFileResponse(csvData, filename, "text/csv");
    }

    /**
     * Export settlement report as Excel
     */
    @GetMapping("/settlement/export/excel")
    public ResponseEntity<byte[]> exportSettlementReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            @RequestParam(defaultValue = "GTBank") String bankName) throws IOException {
        
        log.info("Exporting settlement report as Excel for {} - {}", settlementDate, bankName);
        
        SettlementReport report = reportService.generateSettlementReport(settlementDate, bankName);
        byte[] excelData = excelReportGenerator.generateSettlementReportExcel(report);
        
        String filename = "settlement-report-" + bankName.toLowerCase().replace(" ", "-") + 
            "-" + settlementDate.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        return createFileResponse(excelData, filename, 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Export settlement report as CSV
     */
    @GetMapping("/settlement/export/csv")
    public ResponseEntity<byte[]> exportSettlementReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settlementDate,
            @RequestParam(defaultValue = "GTBank") String bankName) throws IOException {
        
        log.info("Exporting settlement report as CSV for {} - {}", settlementDate, bankName);
        
        SettlementReport report = reportService.generateSettlementReport(settlementDate, bankName);
        byte[] csvData = csvReportGenerator.generateSettlementReportCsv(report);
        
        String filename = "settlement-report-" + bankName.toLowerCase().replace(" ", "-") + 
            "-" + settlementDate.format(DateTimeFormatter.ISO_DATE) + ".csv";
        return createFileResponse(csvData, filename, "text/csv");
    }

    /**
     * Export audit trail as Excel
     */
    @GetMapping("/audit-trail/export/excel")
    public ResponseEntity<byte[]> exportAuditTrailExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        log.info("Exporting audit trail as Excel for {} to {}", startDate, endDate);
        
        AuditTrailReport report = reportService.generateAuditTrailReport(startDate, endDate);
        byte[] excelData = excelReportGenerator.generateAuditTrailExcel(report);
        
        String filename = "audit-trail-" + startDate.format(DateTimeFormatter.ISO_DATE) + 
            "-to-" + endDate.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        return createFileResponse(excelData, filename, 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * Export audit trail as CSV
     */
    @GetMapping("/audit-trail/export/csv")
    public ResponseEntity<byte[]> exportAuditTrailCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        log.info("Exporting audit trail as CSV for {} to {}", startDate, endDate);
        
        AuditTrailReport report = reportService.generateAuditTrailReport(startDate, endDate);
        byte[] csvData = csvReportGenerator.generateAuditTrailCsv(report);
        
        String filename = "audit-trail-" + startDate.format(DateTimeFormatter.ISO_DATE) + 
            "-to-" + endDate.format(DateTimeFormatter.ISO_DATE) + ".csv";
        return createFileResponse(csvData, filename, "text/csv");
    }

    /**
     * Get available report types
     */
    @GetMapping("/types")
    public ResponseEntity<ReportType[]> getReportTypes() {
        return ResponseEntity.ok(ReportType.values());
    }

    /**
     * Get available export formats
     */
    @GetMapping("/formats")
    public ResponseEntity<ExportFormat[]> getExportFormats() {
        return ResponseEntity.ok(ExportFormat.values());
    }

    // Helper method
    
    private ResponseEntity<byte[]> createFileResponse(byte[] data, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(data.length);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }
}
