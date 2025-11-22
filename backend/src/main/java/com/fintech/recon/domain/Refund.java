package com.fintech.recon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "dispute_id")
    private UUID disputeId;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "NGN";

    @Column(length = 50)
    private String status; // 'PENDING', 'SUCCESS', 'FAILED'

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
