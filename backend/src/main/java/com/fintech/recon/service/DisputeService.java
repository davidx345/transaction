package com.fintech.recon.service;

import com.fintech.recon.domain.Reconciliation;
import com.fintech.recon.infrastructure.ReconciliationRepository;
import com.fintech.recon.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final ReconciliationRepository reconciliationRepository;
    private final RefundService refundService;
    private final SecurityUtils securityUtils;

    public List<Reconciliation> getDisputes(String status) {
        UUID userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("No authenticated user found, returning empty list");
            return List.of();
        }
        
        List<Reconciliation> userReconciliations = reconciliationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return userReconciliations.stream()
                .filter(r -> status == null || r.getState().equalsIgnoreCase(status))
                .toList();
    }

    public Reconciliation getDispute(UUID id) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
        
        // Verify user owns this reconciliation
        UUID userId = securityUtils.getCurrentUserId();
        if (userId != null && !userId.equals(reconciliation.getUserId())) {
            throw new RuntimeException("Access denied to this dispute");
        }
        
        return reconciliation;
    }

    @Transactional
    public Reconciliation approveDispute(UUID id, String reason) {
        Reconciliation dispute = getDispute(id);
        validateTransition(dispute.getState(), "APPROVED");
        
        dispute.setState("APPROVED");
        dispute.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Add audit trail entry
        Reconciliation.AuditEntry auditEntry = Reconciliation.AuditEntry.builder()
                .timestamp(java.time.LocalDateTime.now())
                .action("APPROVED")
                .actor("system") // TODO: Extract from security context
                .reason(reason)
                .build();
        dispute.getAuditTrail().add(auditEntry);
        
        log.info("Dispute {} approved. Reason: {}", id, reason);
        
        // Trigger Refund if applicable (Simplified logic: if it's a dispute, we assume refund needed for MVP)
        // In reality, we'd check the discrepancy type (e.g., Double Debit)
        try {
            // Mock amount for now, or fetch from original transaction
            BigDecimal amount = new BigDecimal("5000.00"); 
            String idempotencyKey = "refund-" + id.toString();
            refundService.initiateRefund(id, dispute.getTransactionRef(), amount, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to initiate refund for dispute {}", id, e);
            // Decide if we should rollback approval or just log error
        }

        return reconciliationRepository.save(dispute);
    }

    @Transactional
    public Reconciliation rejectDispute(UUID id, String reason) {
        Reconciliation dispute = getDispute(id);
        validateTransition(dispute.getState(), "REJECTED");
        
        dispute.setState("REJECTED");
        dispute.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Add audit trail entry
        Reconciliation.AuditEntry auditEntry = Reconciliation.AuditEntry.builder()
                .timestamp(java.time.LocalDateTime.now())
                .action("REJECTED")
                .actor("system") // TODO: Extract from security context
                .reason(reason)
                .build();
        dispute.getAuditTrail().add(auditEntry);
        
        log.info("Dispute {} rejected. Reason: {}", id, reason);
        
        return reconciliationRepository.save(dispute);
    }

    private void validateTransition(String currentState, String newState) {
        // Simple state machine validation
        if ("APPROVED".equals(currentState) || "REJECTED".equals(currentState)) {
            throw new IllegalStateException("Cannot transition from terminal state: " + currentState);
        }
    }
}
