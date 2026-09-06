package com.acme.claims.repository;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Optional<Claim> findByClaimNumber(String claimNumber);

    boolean existsByClaimNumber(String claimNumber);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    Page<Claim> findByPolicyNumber(String policyNumber, Pageable pageable);

    Page<Claim> findByAssignedTo(String assignedTo, Pageable pageable);

    @Query("SELECT c FROM Claim c WHERE c.createdAt BETWEEN :start AND :end")
    Page<Claim> findByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT c FROM Claim c WHERE c.status = :status AND c.triagePriority >= :minPriority")
    List<Claim> findHighPriorityClaimsByStatus(@Param("status") ClaimStatus status,
                                               @Param("minPriority") int minPriority);

    long countByStatus(ClaimStatus status);
}
