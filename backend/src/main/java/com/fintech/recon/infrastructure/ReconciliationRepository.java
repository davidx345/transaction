package com.fintech.recon.infrastructure;

import com.fintech.recon.domain.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {
    List<Reconciliation> findByTransactionRef(String transactionRef);
    
    List<Reconciliation> findByUserId(UUID userId);
    
    List<Reconciliation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<Reconciliation> findByUserIdAndState(UUID userId, String state);
    
    java.util.Optional<Reconciliation> findByTransactionRefAndUserId(String transactionRef, UUID userId);
}
