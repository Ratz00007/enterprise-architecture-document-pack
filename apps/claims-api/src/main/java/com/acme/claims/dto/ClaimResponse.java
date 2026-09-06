package com.acme.claims.dto;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimStatus;
import com.acme.claims.domain.ClaimType;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    String claimNumber,
    String policyNumber,
    ClaimStatus status,
    ClaimType claimType,
    Instant incidentDate,
    Instant reportedDate,
    String description,
    Long estimatedAmountMinor,
    Long approvedAmountMinor,
    Long paidAmountMinor,
    String assignedTo,
    Integer triagePriority,
    Double triageScore,
    String adjudicationNotes,
    String rejectionReason,
    String payoutReference,
    Instant payoutDate,
    Long version,
    Instant createdAt,
    Instant updatedAt,
    String createdBy,
    String updatedBy
) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
            claim.getId(),
            claim.getClaimNumber(),
            claim.getPolicyNumber(),
            claim.getStatus(),
            claim.getClaimType(),
            claim.getIncidentDate(),
            claim.getReportedDate(),
            claim.getDescription(),
            claim.getEstimatedAmountMinor(),
            claim.getApprovedAmountMinor(),
            claim.getPaidAmountMinor(),
            claim.getAssignedTo(),
            claim.getTriagePriority(),
            claim.getTriageScore(),
            claim.getAdjudicationNotes(),
            claim.getRejectionReason(),
            claim.getPayoutReference(),
            claim.getPayoutDate(),
            claim.getVersion(),
            claim.getCreatedAt(),
            claim.getUpdatedAt(),
            claim.getCreatedBy(),
            claim.getUpdatedBy()
        );
    }
}
