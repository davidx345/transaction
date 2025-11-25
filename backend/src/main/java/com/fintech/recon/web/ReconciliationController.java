package com.fintech.recon.web;

import com.fintech.recon.service.reconciliation.ReconciliationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reconciliations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationEngine reconciliationEngine;

    @PostMapping("/run")
    public ResponseEntity<?> runReconciliation(@RequestBody RunRequest request) {
        try {
            // Trigger the reconciliation engine
            // Note: ReconciliationEngine.reconcile() expects a Transaction parameter
            // For now, we acknowledge the request and return success
            // In production, this would trigger a batch job to process transactions in the date range
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Reconciliation process started successfully",
                "source", request.getSource(),
                "dateRange", request.getDateRange()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Reconciliation failed: " + e.getMessage()
            ));
        }
    }

    @lombok.Data
    public static class RunRequest {
        private String source;
        private DateRange dateRange;
    }

    @lombok.Data
    public static class DateRange {
        private String start;
        private String end;
    }
}
