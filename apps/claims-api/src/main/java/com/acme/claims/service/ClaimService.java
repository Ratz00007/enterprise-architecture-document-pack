package com.acme.claims.service;

import com.acme.claims.model.Claim;
import com.acme.claims.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Claim Service - Business logic for claim processing
 * 
 * Handles the complete workflow: FNOL → Triage → Adjudication → Payout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public Page<Claim> getAllClaims(Pageable pageable) {
        return claimRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Claim> getClaimById(UUID id) {
        return claimRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Claim> getClaimByNumber(String claimNumber) {
        return claimRepository.findAll()
            .stream()
            .filter(c -> c.getClaimNumber().equals(claimNumber))
            .findFirst();
    }

    @Transactional
    public Claim createClaim(Claim claim, String userId) {
        claim.setStatus(Claim.ClaimStatus.FNOL);
        claim.setCreatedBy(userId);
        claim.setCreatedAt(LocalDateTime.now());
        log.info("Creating new claim: {}", claim.getClaimNumber());
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim updateClaim(UUID id, Claim updatedClaim, String userId) {
        return claimRepository.findById(id)
            .map(existingClaim -> {
                existingClaim.setStatus(updatedClaim.getStatus());
                existingClaim.setDescription(updatedClaim.getDescription());
                existingClaim.setEstimatedAmount(updatedClaim.getEstimatedAmount());
                existingClaim.setAssignedTo(updatedClaim.getAssignedTo());
                existingClaim.setUpdatedBy(userId);
                log.info("Updating claim: {}", id);
                return claimRepository.save(existingClaim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim transitionToTriage(UUID id, int priority, Double score, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.TRIAGE);
                claim.setTriagePriority(priority);
                claim.setTriageScore(score);
                claim.setUpdatedBy(userId);
                log.info("Claim {} transitioned to TRIAGE with priority: {}", id, priority);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim transitionToAdjudication(UUID id, String notes, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.ADJUDICATION);
                claim.setAdjudicationNotes(notes);
                claim.setUpdatedBy(userId);
                log.info("Claim {} transitioned to ADJUDICATION", id);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim approveClaim(UUID id, BigDecimal approvedAmount, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.APPROVED);
                claim.setApprovedAmount(approvedAmount);
                claim.setUpdatedBy(userId);
                log.info("Claim {} approved with amount: {}", id, approvedAmount);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim rejectClaim(UUID id, String rejectionReason, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.REJECTED);
                claim.setAdjudicationNotes(rejectionReason);
                claim.setUpdatedBy(userId);
                log.info("Claim {} rejected. Reason: {}", id, rejectionReason);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim processPayout(UUID id, String payoutReference, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.PAYOUT);
                claim.setPayoutReference(payoutReference);
                claim.setPayoutDate(LocalDateTime.now());
                claim.setUpdatedBy(userId);
                log.info("Payout processing initiated for claim: {} with reference: {}", id, payoutReference);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim completePayout(UUID id, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.PAID);
                claim.setPaidAmount(claim.getApprovedAmount());
                claim.setUpdatedBy(userId);
                log.info("Payout completed for claim: {}", id);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional
    public Claim closeClaim(UUID id, String userId) {
        return claimRepository.findById(id)
            .map(claim -> {
                claim.setStatus(Claim.ClaimStatus.CLOSED);
                claim.setUpdatedBy(userId);
                log.info("Claim {} closed", id);
                return claimRepository.save(claim);
            })
            .orElseThrow(() -> new RuntimeException("Claim not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Claim> getClaimsByStatus(Claim.ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Claim> getHighPriorityClaims(Claim.ClaimStatus status, int minPriority) {
        return claimRepository.findHighPriorityClaimsByStatus(status, minPriority);
    }

    @Transactional(readOnly = true)
    public long getClaimCountByStatus(Claim.ClaimStatus status) {
        return claimRepository.countByStatus(status);
    }

    @Transactional
    public void deleteClaim(UUID id) {
        if (!claimRepository.existsById(id)) {
            throw new RuntimeException("Claim not found with id: " + id);
        }
        claimRepository.deleteById(id);
        log.info("Claim {} deleted", id);
    }
}
