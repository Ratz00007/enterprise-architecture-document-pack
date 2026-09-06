package com.acme.claims.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Claim Entity - Core domain object representing an insurance claim
 * 
 * Supports the complete workflow: FNOL → Triage → Adjudication → Payout
 */
@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_status", columnList = "status"),
    @Index(name = "idx_claim_policy_number", columnList = "policyNumber"),
    @Index(name = "idx_claim_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claim_number", unique = true, nullable = false)
    private String claimNumber;

    @Column(name = "policy_number", nullable = false)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false)
    private ClaimType claimType;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "reported_date", nullable = false)
    private LocalDateTime reportedDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_amount", precision = 15, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "triage_priority")
    private Integer triagePriority;

    @Column(name = "triage_score")
    private Double triageScore;

    @Column(name = "adjudication_notes", columnDefinition = "TEXT")
    private String adjudicationNotes;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "payout_date")
    private LocalDateTime payoutDate;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (claimNumber == null) {
            claimNumber = generateClaimNumber();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateClaimNumber() {
        return "CLM-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public enum ClaimStatus {
        FNOL,           // First Notice of Loss
        TRIAGE,         // Under assessment
        ADJUDICATION,   // Being reviewed for approval
        APPROVED,       // Approved for payout
        REJECTED,       // Rejected
        PAYOUT,         // Payment being processed
        PAID,           // Payment completed
        CLOSED          // Claim closed
    }

    public enum ClaimType {
        AUTO,
        PROPERTY,
        HEALTH,
        LIFE,
        LIABILITY,
        WORKERS_COMP,
        OTHER
    }
}
