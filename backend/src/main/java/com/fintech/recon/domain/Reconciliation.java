package com.fintech.recon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reconciliations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "transaction_ref", unique = true)
    private String transactionRef;

    @Column(name = "run_id")
    private String runId;

    @Column(length = 50)
    private String state; // 'MATCHED', 'DISPUTED', etc.

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "rules_fired", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> rulesFired;

    @Column(name = "audit_trail", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<AuditEntry> auditTrail = new java.util.ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditEntry {
        private LocalDateTime timestamp;
        private String action;
        private String actor;
        private String reason;
    }
}
