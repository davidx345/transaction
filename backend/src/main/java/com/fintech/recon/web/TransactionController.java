package com.fintech.recon.web;

import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.infrastructure.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransactionController {

    private final ReconciliationRepository reconciliationRepository;

    @GetMapping("/compare")
    public ResponseEntity<?> compareTransactions(@RequestParam String ref) {
        try {
            // Find reconciliations matching the transaction reference
            List<Reconciliation> reconciliations = reconciliationRepository
                .findByTransactionRef(ref);

            if (reconciliations.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No transactions found for reference: " + ref
                ));
            }

            // Build comparison response with data from different sources
            Map<String, Object> comparison = new HashMap<>();
            
            for (Reconciliation recon : reconciliations) {
                TransactionData txnData = new TransactionData();
                txnData.setId(recon.getId().toString());
                txnData.setSource(determineSource(recon));
                txnData.setExternalReference(recon.getTransactionRef());
                txnData.setNormalizedReference(recon.getTransactionRef());
                txnData.setAmount(5000.00); // TODO: Extract from rulesFired or add amount field
                txnData.setStatus(recon.getState());
                txnData.setTimestamp(recon.getCreatedAt().toString());
                
                String sourceKey = determineSource(recon);
                comparison.put(sourceKey, txnData);
            }

            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to compare transactions: " + e.getMessage()
            ));
        }
    }

    private String determineSource(Reconciliation recon) {
        // Try to determine source from runId or state
        if (recon.getRunId() != null) {
            if (recon.getRunId().contains("paystack") || recon.getRunId().contains("provider")) {
                return "provider";
            } else if (recon.getRunId().contains("bank") || recon.getRunId().contains("gtb")) {
                return "bank";
            } else if (recon.getRunId().contains("ledger")) {
                return "ledger";
            }
        }
        // Default to provider
        return "provider";
    }

    @lombok.Data
    public static class TransactionData {
        private String id;
        private String source;
        private String externalReference;
        private String normalizedReference;
        private Double amount;
        private String status;
        private String timestamp;
    }
}
