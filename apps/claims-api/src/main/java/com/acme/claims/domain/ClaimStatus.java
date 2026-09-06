package com.acme.claims.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Claim lifecycle states. The architecture pack states the happy path as
 * FNOL → triage → adjudication → payout (README, AGENTS.md); the rejection and
 * closure transitions recorded here are captured in ADR-006.
 */
public enum ClaimStatus {
    FNOL,
    TRIAGE,
    ADJUDICATION,
    APPROVED,
    REJECTED,
    PAYOUT,
    PAID,
    CLOSED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED_TRANSITIONS = Map.of(
        FNOL, EnumSet.of(TRIAGE),
        TRIAGE, EnumSet.of(ADJUDICATION),
        ADJUDICATION, EnumSet.of(APPROVED, REJECTED),
        APPROVED, EnumSet.of(PAYOUT),
        REJECTED, EnumSet.of(CLOSED),
        PAYOUT, EnumSet.of(PAID),
        PAID, EnumSet.of(CLOSED),
        CLOSED, EnumSet.noneOf(ClaimStatus.class)
    );

    public boolean canTransitionTo(ClaimStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(ClaimStatus.class)).contains(target);
    }
}
