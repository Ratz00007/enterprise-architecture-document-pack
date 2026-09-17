package com.acme.claims.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ClaimStateTest {

    @Test
    void new_claim_transitions_to_triage() {
        var c = sampleClaim().toBuilder().state(new ClaimState.New()).build();
        var next = c.transitionTo(ClaimState.Triage.instance,
                reason("triage complete"));
        assertThat(next.state()).isEqualTo(ClaimState.Triage.instance);
    }

    @Test
    void illegal_transition_throws() {
        var c = sampleClaim().toBuilder().state(ClaimState.Triage.instance).build();
        assertThatThrownBy(() -> c.transitionTo(ClaimState.Paid.instance, reason("no")) )
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void closed_can_be_reopened() {
        var c = sampleClaim().toBuilder().state(ClaimState.Closed.instance).build();
        var next = c.transitionTo(ClaimState.Reopened.instance, reason("appeal received"));
        assertThat(next.state()).isEqualTo(ClaimState.Reopened.instance);
    }

    @Test
    void archived_is_terminal() {
        var c = sampleClaim().toBuilder().state(ClaimState.Archived.instance).build();
        assertThatThrownBy(() -> c.transitionTo(ClaimState.Reopened.instance, reason("nope")))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    private static Claim sampleClaim() {
        return Claim.builder()
                .id(UUID.randomUUID())
                .claimNumber("CLM-TEST-1")
                .policyId(UUID.randomUUID())
                .claimantPartyId(UUID.randomUUID())
                .state(new ClaimState.New())
                .lossDate(Instant.parse("2026-01-01T00:00:00Z"))
                .reportedDate(Instant.parse("2026-01-01T01:00:00Z"))
                .estimatedAmountCents(0L)
                .currency("USD")
                .version(1L)
                .build();
    }

    private static TransitionReason reason(String comment) {
        return new TransitionReason(UUID.randomUUID(), comment, null, null);
    }
}
