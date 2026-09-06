package com.acme.claims.service;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimNotFoundException;
import com.acme.claims.domain.ClaimStatus;
import com.acme.claims.domain.ClaimType;
import com.acme.claims.domain.InvalidClaimTransitionException;
import com.acme.claims.dto.CreateClaimRequest;
import com.acme.claims.repository.ClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claims;

    @InjectMocks
    private ClaimService service;

    private static Claim claimIn(ClaimStatus status) {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-20260906-ABC123");
        claim.setPolicyNumber("POL-1000");
        claim.setClaimType(ClaimType.AUTO);
        claim.setIncidentDate(Instant.parse("2026-09-01T10:00:00Z"));
        claim.setReportedDate(Instant.parse("2026-09-02T09:00:00Z"));
        claim.setStatus(status);
        return claim;
    }

    private static CreateClaimRequest createRequest() {
        return new CreateClaimRequest("POL-1000", ClaimType.AUTO,
            Instant.parse("2026-09-01T10:00:00Z"), "Rear-end collision", 250_000L);
    }

    @Test
    void createClaimStartsInFnolWithClaimNumberAndActor() {
        when(claims.existsByClaimNumber(anyString())).thenReturn(false);
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim created = service.createClaim(createRequest(), "adjuster-1");

        assertThat(created.getStatus()).isEqualTo(ClaimStatus.FNOL);
        assertThat(created.getClaimNumber()).startsWith("CLM-");
        assertThat(created.getCreatedBy()).isEqualTo("adjuster-1");
        assertThat(created.getReportedDate()).isNotNull();
        assertThat(created.getEstimatedAmountMinor()).isEqualTo(250_000L);
    }

    @Test
    void triageMovesFnolClaimAndStoresAssessment() {
        Claim claim = claimIn(ClaimStatus.FNOL);
        when(claims.findById(any())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim triaged = service.triage(claim.getId(),
            new com.acme.claims.dto.TriageRequest(3, 72.5, "adjuster-2"), "adjuster-2");

        assertThat(triaged.getStatus()).isEqualTo(ClaimStatus.TRIAGE);
        assertThat(triaged.getTriagePriority()).isEqualTo(3);
        assertThat(triaged.getTriageScore()).isEqualTo(72.5);
        assertThat(triaged.getAssignedTo()).isEqualTo("adjuster-2");
        assertThat(triaged.getUpdatedBy()).isEqualTo("adjuster-2");
    }

    @Test
    void triageRejectsClaimThatIsNotInFnol() {
        Claim claim = claimIn(ClaimStatus.ADJUDICATION);
        when(claims.findById(any())).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.triage(claim.getId(),
            new com.acme.claims.dto.TriageRequest(1, 10.0, null), "adjuster-1"))
            .isInstanceOf(InvalidClaimTransitionException.class)
            .hasMessageContaining("ADJUDICATION");
    }

    @Test
    void approveStoresApprovedAmount() {
        Claim claim = claimIn(ClaimStatus.ADJUDICATION);
        when(claims.findById(any())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim approved = service.approve(claim.getId(),
            new com.acme.claims.dto.ApprovalRequest(200_000L), "approver-1");

        assertThat(approved.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(approved.getApprovedAmountMinor()).isEqualTo(200_000L);
    }

    @Test
    void rejectStoresReason() {
        Claim claim = claimIn(ClaimStatus.ADJUDICATION);
        when(claims.findById(any())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim rejected = service.reject(claim.getId(),
            new com.acme.claims.dto.RejectionRequest("Policy lapsed before incident"), "approver-1");

        assertThat(rejected.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Policy lapsed before incident");
    }

    @Test
    void completePayoutCopiesApprovedAmountToPaid() {
        Claim claim = claimIn(ClaimStatus.PAYOUT);
        claim.setApprovedAmountMinor(200_000L);
        when(claims.findById(any())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim paid = service.completePayout(claim.getId(), "payments-1");

        assertThat(paid.getStatus()).isEqualTo(ClaimStatus.PAID);
        assertThat(paid.getPaidAmountMinor()).isEqualTo(200_000L);
    }

    @Test
    void closeWorksFromRejectedButNotFromApproved() {
        Claim rejected = claimIn(ClaimStatus.REJECTED);
        when(claims.findById(rejected.getId())).thenReturn(Optional.of(rejected));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.close(rejected.getId(), "adjuster-1").getStatus()).isEqualTo(ClaimStatus.CLOSED);

        Claim approved = claimIn(ClaimStatus.APPROVED);
        when(claims.findById(approved.getId())).thenReturn(Optional.of(approved));
        assertThatThrownBy(() -> service.close(approved.getId(), "adjuster-1"))
            .isInstanceOf(InvalidClaimTransitionException.class);
    }

    @Test
    void getByIdThrowsWhenClaimIsMissing() {
        when(claims.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
            .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void getByClaimNumberReturnsMatchingClaim() {
        Claim claim = claimIn(ClaimStatus.FNOL);
        when(claims.findByClaimNumber("CLM-20260906-ABC123")).thenReturn(Optional.of(claim));

        assertThat(service.getByClaimNumber("CLM-20260906-ABC123")).isSameAs(claim);
        assertThatThrownBy(() -> service.getByClaimNumber("CLM-NOPE"))
            .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void listFiltersByStatusOnlyWhenStatusIsProvided() {
        when(claims.findAll(org.springframework.data.domain.PageRequest.of(0, 20)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        when(claims.findByStatus(ClaimStatus.PAID, org.springframework.data.domain.PageRequest.of(0, 20)))
            .thenReturn(org.springframework.data.domain.Page.empty());

        assertThat(service.list(null, org.springframework.data.domain.PageRequest.of(0, 20))).isEmpty();
        verify(claims).findAll(org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(service.list(ClaimStatus.PAID, org.springframework.data.domain.PageRequest.of(0, 20))).isEmpty();
        verify(claims).findByStatus(ClaimStatus.PAID, org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Test
    void beginAdjudicationStoresNotes() {
        Claim claim = claimIn(ClaimStatus.TRIAGE);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim adjudicating = service.beginAdjudication(claim.getId(),
            new com.acme.claims.dto.AdjudicationRequest("Liability confirmed"), "adjuster-1");

        assertThat(adjudicating.getStatus()).isEqualTo(ClaimStatus.ADJUDICATION);
        assertThat(adjudicating.getAdjudicationNotes()).isEqualTo("Liability confirmed");
    }

    @Test
    void payoutStoresReferenceAndDate() {
        Claim claim = claimIn(ClaimStatus.APPROVED);
        when(claims.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim paying = service.payout(claim.getId(),
            new com.acme.claims.dto.PayoutRequest("PAY-2026-0001"), "payments-1");

        assertThat(paying.getStatus()).isEqualTo(ClaimStatus.PAYOUT);
        assertThat(paying.getPayoutReference()).isEqualTo("PAY-2026-0001");
        assertThat(paying.getPayoutDate()).isNotNull();
    }

    @Test
    void claimNumberCollisionsAreRetried() {
        when(claims.existsByClaimNumber(anyString())).thenReturn(true, true, false);
        when(claims.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Claim created = service.createClaim(createRequest(), "adjuster-1");

        assertThat(created.getClaimNumber()).startsWith("CLM-");
        verify(claims, org.mockito.Mockito.times(3)).existsByClaimNumber(anyString());
    }

    @Test
    void claimNumberGenerationGivesUpAfterThreeCollisions() {
        when(claims.existsByClaimNumber(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createClaim(createRequest(), "adjuster-1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("claim number");
    }
}
