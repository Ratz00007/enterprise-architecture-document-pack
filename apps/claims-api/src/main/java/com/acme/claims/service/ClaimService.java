package com.acme.claims.service;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimNotFoundException;
import com.acme.claims.domain.ClaimStatus;
import com.acme.claims.dto.AdjudicationRequest;
import com.acme.claims.dto.ApprovalRequest;
import com.acme.claims.dto.CreateClaimRequest;
import com.acme.claims.dto.PayoutRequest;
import com.acme.claims.dto.RejectionRequest;
import com.acme.claims.dto.TriageRequest;
import com.acme.claims.repository.ClaimRepository;
import com.acme.claims.util.UuidV7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Claim lifecycle business logic. Every state change validates against the
 * {@link ClaimStatus} state machine; there is no delete operation because
 * claims are part of the audit trail.
 */
@Service
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);
    private static final DateTimeFormatter CLAIM_NUMBER_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT).withZone(ZoneOffset.UTC);

    private final ClaimRepository claims;

    public ClaimService(ClaimRepository claims) {
        this.claims = claims;
    }

    @Transactional(readOnly = true)
    public Claim getById(UUID id) {
        return claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Claim getByClaimNumber(String claimNumber) {
        return claims.findByClaimNumber(claimNumber).orElseThrow(() -> new ClaimNotFoundException(claimNumber));
    }

    @Transactional(readOnly = true)
    public Page<Claim> list(ClaimStatus status, Pageable pageable) {
        return status == null ? claims.findAll(pageable) : claims.findByStatus(status, pageable);
    }

    @Transactional
    public Claim createClaim(CreateClaimRequest request, String userId) {
        Claim claim = new Claim();
        claim.setClaimNumber(nextClaimNumber());
        claim.setPolicyNumber(request.policyNumber());
        claim.setClaimType(request.claimType());
        claim.setIncidentDate(request.incidentDate());
        claim.setReportedDate(Instant.now());
        claim.setDescription(request.description());
        claim.setEstimatedAmountMinor(request.estimatedAmountMinor());
        claim.setStatus(ClaimStatus.FNOL);
        claim.setCreatedBy(userId);
        log.info("Claim {} created for policy {} by {}", claim.getClaimNumber(), claim.getPolicyNumber(), userId);
        return claims.save(claim);
    }

    @Transactional
    public Claim triage(UUID id, TriageRequest request, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.TRIAGE);
        claim.setTriagePriority(request.priority());
        claim.setTriageScore(request.score());
        claim.setAssignedTo(request.assignedTo());
        claim.setUpdatedBy(userId);
        log.info("Claim {} moved to TRIAGE with priority {}", claim.getClaimNumber(), request.priority());
        return claims.save(claim);
    }

    @Transactional
    public Claim beginAdjudication(UUID id, AdjudicationRequest request, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.ADJUDICATION);
        claim.setAdjudicationNotes(request.notes());
        claim.setUpdatedBy(userId);
        log.info("Claim {} moved to ADJUDICATION", claim.getClaimNumber());
        return claims.save(claim);
    }

    @Transactional
    public Claim approve(UUID id, ApprovalRequest request, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.APPROVED);
        claim.setApprovedAmountMinor(request.approvedAmountMinor());
        claim.setUpdatedBy(userId);
        log.info("Claim {} approved for {} minor units", claim.getClaimNumber(), request.approvedAmountMinor());
        return claims.save(claim);
    }

    @Transactional
    public Claim reject(UUID id, RejectionRequest request, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.REJECTED);
        claim.setRejectionReason(request.reason());
        claim.setUpdatedBy(userId);
        log.info("Claim {} rejected", claim.getClaimNumber());
        return claims.save(claim);
    }

    @Transactional
    public Claim payout(UUID id, PayoutRequest request, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.PAYOUT);
        claim.setPayoutReference(request.payoutReference());
        claim.setPayoutDate(Instant.now());
        claim.setUpdatedBy(userId);
        log.info("Claim {} payout initiated (reference {})", claim.getClaimNumber(), request.payoutReference());
        return claims.save(claim);
    }

    @Transactional
    public Claim completePayout(UUID id, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.PAID);
        claim.setPaidAmountMinor(claim.getApprovedAmountMinor());
        claim.setUpdatedBy(userId);
        log.info("Claim {} payout completed", claim.getClaimNumber());
        return claims.save(claim);
    }

    @Transactional
    public Claim close(UUID id, String userId) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        claim.transitionTo(ClaimStatus.CLOSED);
        claim.setUpdatedBy(userId);
        log.info("Claim {} closed", claim.getClaimNumber());
        return claims.save(claim);
    }

    private String nextClaimNumber() {
        String date = CLAIM_NUMBER_DATE.format(Instant.now());
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = "CLM-%s-%s".formatted(date, randomSuffix());
            if (!claims.existsByClaimNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique claim number");
    }

    private String randomSuffix() {
        String hex = UuidV7.randomUUID().toString().replace("-", "");
        return hex.substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
