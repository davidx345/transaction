package com.fintech.recon.service;

import com.fintech.recon.domain.Refund;
import com.fintech.recon.infrastructure.RefundRepository;
// import com.fintech.recon.service.ingestion.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    // In a real app, we'd select the correct provider based on the transaction
    // For MVP, we might need a way to inject a specific one or a factory
    // private final PaymentProvider paymentProvider; 

    @Transactional
    public Refund initiateRefund(UUID disputeId, String transactionRef, BigDecimal amount, String idempotencyKey) {
        // 1. Idempotency Check
        Optional<Refund> existing = refundRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent refund request found for key: {}", idempotencyKey);
            return existing.get();
        }

        // 2. Create Pending Refund Record
        Refund refund = Refund.builder()
                .disputeId(disputeId)
                .transactionRef(transactionRef)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .status("PENDING")
                .build();
        
        refund = refundRepository.save(refund);

        // 3. Call Provider (Simulated for MVP as we don't have a concrete Provider implementation yet)
        boolean success = simulateProviderRefund(transactionRef, amount);

        // 4. Update Status
        if (success) {
            refund.setStatus("SUCCESS");
            refund.setExecutedAt(LocalDateTime.now());
            refund.setProviderReference("REF-" + UUID.randomUUID().toString().substring(0, 8));
        } else {
            refund.setStatus("FAILED");
        }

        return refundRepository.save(refund);
    }

    private boolean simulateProviderRefund(String ref, BigDecimal amount) {
        log.info("Calling Payment Provider to refund {} for transaction {}", amount, ref);
        return true; // Simulate success
    }
}
