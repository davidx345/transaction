package com.fintech.recon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions_raw")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String source; // 'paystack' | 'bank' | 'ledger'

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "normalized_reference")
    private String normalizedReference;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "NGN";

    @Column(length = 50)
    private String status;

    @Column(name = "customer_identifier")
    private String customerIdentifier;

    private LocalDateTime timestamp;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> rawData;

    @Column(name = "ingested_at")
    @Builder.Default
    private LocalDateTime ingestedAt = LocalDateTime.now();
}
