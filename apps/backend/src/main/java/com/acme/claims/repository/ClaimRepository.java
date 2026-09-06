package com.acme.claims.repository;

import com.acme.claims.model.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Claim Repository - Data access layer for claims
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Page<Claim> findByStatus(Claim.ClaimStatus status, Pageable pageable);

    Page<Claim> findByPolicyNumber(String policyNumber, Pageable pageable);

    List<Claim> findByAssignedTo(String assignedTo);

    @Query("SELECT c FROM Claim c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    Page<Claim> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT c FROM Claim c WHERE c.status = :status AND c.triagePriority >= :minPriority")
    List<Claim> findHighPriorityClaimsByStatus(
        @Param("status") Claim.ClaimStatus status,
        @Param("minPriority") int minPriority
    );

    @Query("SELECT COUNT(c) FROM Claim c WHERE c.status = :status")
    long countByStatus(@Param("status") Claim.ClaimStatus status);

    @Query("SELECT c FROM Claim c WHERE c.updatedAt < :threshold AND c.status IN :statuses")
    List<Claim> findStaleClaims(
        @Param("threshold") LocalDateTime threshold,
        @Param("statuses") List<Claim.ClaimStatus> statuses
    );

    boolean existsByClaimNumber(String claimNumber);
}
