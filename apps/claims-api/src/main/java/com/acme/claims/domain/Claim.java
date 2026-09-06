package com.acme.claims.domain;

import com.acme.claims.util.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Core claim aggregate. Money values are stored as BIGINT minor units
 * (ADR-007); all timestamps are UTC {@link Instant}s; primary keys are
 * UUIDv7 (ADR-009).
 */
@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_status", columnList = "status"),
    @Index(name = "idx_claim_policy_number", columnList = "policyNumber"),
    @Index(name = "idx_claim_created_at", columnList = "createdAt")
})
public class Claim {

    @Id
    private UUID id;

    @Column(name = "claim_number", unique = true, nullable = false, updatable = false, length = 50)
    private String claimNumber;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 20)
    private ClaimType claimType;

    @Column(name = "incident_date", nullable = false)
    private Instant incidentDate;

    @Column(name = "reported_date", nullable = false, updatable = false)
    private Instant reportedDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_amount")
    private Long estimatedAmountMinor;

    @Column(name = "approved_amount")
    private Long approvedAmountMinor;

    @Column(name = "paid_amount")
    private Long paidAmountMinor;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "triage_priority")
    private Integer triagePriority;

    @Column(name = "triage_score")
    private Double triageScore;

    @Column(name = "adjudication_notes", columnDefinition = "TEXT")
    private String adjudicationNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Column(name = "payout_date")
    private Instant payoutDate;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public Claim() {
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UuidV7.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Applies a lifecycle transition after validating it against the state
     * machine; every state change must go through this method.
     */
    public void transitionTo(ClaimStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidClaimTransitionException(status, target);
        }
        status = target;
    }

    public UUID getId() {
        return id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public ClaimType getClaimType() {
        return claimType;
    }

    public void setClaimType(ClaimType claimType) {
        this.claimType = claimType;
    }

    public Instant getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(Instant incidentDate) {
        this.incidentDate = incidentDate;
    }

    public Instant getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(Instant reportedDate) {
        this.reportedDate = reportedDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getEstimatedAmountMinor() {
        return estimatedAmountMinor;
    }

    public void setEstimatedAmountMinor(Long estimatedAmountMinor) {
        this.estimatedAmountMinor = estimatedAmountMinor;
    }

    public Long getApprovedAmountMinor() {
        return approvedAmountMinor;
    }

    public void setApprovedAmountMinor(Long approvedAmountMinor) {
        this.approvedAmountMinor = approvedAmountMinor;
    }

    public Long getPaidAmountMinor() {
        return paidAmountMinor;
    }

    public void setPaidAmountMinor(Long paidAmountMinor) {
        this.paidAmountMinor = paidAmountMinor;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Integer getTriagePriority() {
        return triagePriority;
    }

    public void setTriagePriority(Integer triagePriority) {
        this.triagePriority = triagePriority;
    }

    public Double getTriageScore() {
        return triageScore;
    }

    public void setTriageScore(Double triageScore) {
        this.triageScore = triageScore;
    }

    public String getAdjudicationNotes() {
        return adjudicationNotes;
    }

    public void setAdjudicationNotes(String adjudicationNotes) {
        this.adjudicationNotes = adjudicationNotes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getPayoutReference() {
        return payoutReference;
    }

    public void setPayoutReference(String payoutReference) {
        this.payoutReference = payoutReference;
    }

    public Instant getPayoutDate() {
        return payoutDate;
    }

    public void setPayoutDate(Instant payoutDate) {
        this.payoutDate = payoutDate;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
