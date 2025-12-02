package com.fintech.recon.infrastructure;

import com.fintech.recon.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find transaction by exact normalized reference and source
     */
    Optional<Transaction> findByNormalizedReferenceAndSource(String normalizedReference, String source);

    /**
     * Find all transactions by normalized reference (across all sources)
     */
    List<Transaction> findByNormalizedReference(String normalizedReference);

    /**
     * Find transactions by source type
     */
    List<Transaction> findBySource(String source);

    /**
     * Find transactions within date range for a specific source
     */
    List<Transaction> findBySourceAndTimestampBetween(String source, LocalDateTime start, LocalDateTime end);

    /**
     * Find potential matches by similar reference (fuzzy match using LIKE)
     */
    @Query("SELECT t FROM Transaction t WHERE t.source = :source AND " +
           "LOWER(REPLACE(REPLACE(REPLACE(t.normalizedReference, '-', ''), '_', ''), ' ', '')) " +
           "LIKE LOWER(CONCAT('%', :cleanedReference, '%'))")
    List<Transaction> findByFuzzyReference(@Param("source") String source, 
                                           @Param("cleanedReference") String cleanedReference);

    /**
     * Find transactions by amount range (for tolerance matching)
     */
    @Query("SELECT t FROM Transaction t WHERE t.source = :source AND " +
           "t.amount BETWEEN :minAmount AND :maxAmount")
    List<Transaction> findByAmountRange(@Param("source") String source,
                                        @Param("minAmount") BigDecimal minAmount,
                                        @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find transactions by amount range and date range (optimized matching query)
     */
    @Query("SELECT t FROM Transaction t WHERE t.source = :source AND " +
           "t.amount BETWEEN :minAmount AND :maxAmount AND " +
           "t.timestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findPotentialMatches(@Param("source") String source,
                                           @Param("minAmount") BigDecimal minAmount,
                                           @Param("maxAmount") BigDecimal maxAmount,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find potential duplicates - same amount and similar timestamp
     */
    @Query("SELECT t FROM Transaction t WHERE t.source = :source AND " +
           "t.amount = :amount AND t.timestamp BETWEEN :startDate AND :endDate AND " +
           "t.id != :excludeId")
    List<Transaction> findPotentialDuplicates(@Param("source") String source,
                                              @Param("amount") BigDecimal amount,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("excludeId") UUID excludeId);

    /**
     * Count transactions by source and status
     */
    long countBySourceAndStatus(String source, String status);

    /**
     * Find unreconciled transactions (not yet processed)
     */
    @Query("SELECT t FROM Transaction t WHERE t.source = :source AND " +
           "t.normalizedReference NOT IN (SELECT r.transactionRef FROM Reconciliation r)")
    List<Transaction> findUnreconciledBySource(@Param("source") String source);

    /**
     * Find all transactions by userId
     */
    List<Transaction> findByUserId(UUID userId);

    /**
     * Find transactions by userId and source
     */
    List<Transaction> findByUserIdAndSource(UUID userId, String source);
}
