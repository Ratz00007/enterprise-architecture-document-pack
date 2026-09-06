package com.acme.claims.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimStatusTest {

    private static final Set<String> LEGAL = Set.of(
        "FNOL>TRIAGE",
        "TRIAGE>ADJUDICATION",
        "ADJUDICATION>APPROVED",
        "ADJUDICATION>REJECTED",
        "APPROVED>PAYOUT",
        "PAYOUT>PAID",
        "PAID>CLOSED",
        "REJECTED>CLOSED"
    );

    @Test
    void legalTransitionsAreAccepted() {
        assertAll(LEGAL.stream().map(pair -> pair.split(">")).map(parts -> () ->
            assertTrue(ClaimStatus.valueOf(parts[0]).canTransitionTo(ClaimStatus.valueOf(parts[1])),
                "%s -> %s must be legal".formatted(parts[0], parts[1]))));
    }

    @Test
    void shortcutsAndBackwardsTransitionsAreRejected() {
        assertAll(
            () -> assertFalse(ClaimStatus.FNOL.canTransitionTo(ClaimStatus.APPROVED)),
            () -> assertFalse(ClaimStatus.FNOL.canTransitionTo(ClaimStatus.CLOSED)),
            () -> assertFalse(ClaimStatus.TRIAGE.canTransitionTo(ClaimStatus.PAYOUT)),
            () -> assertFalse(ClaimStatus.ADJUDICATION.canTransitionTo(ClaimStatus.PAID)),
            () -> assertFalse(ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.TRIAGE)),
            () -> assertFalse(ClaimStatus.PAID.canTransitionTo(ClaimStatus.FNOL)),
            () -> assertFalse(ClaimStatus.REJECTED.canTransitionTo(ClaimStatus.APPROVED))
        );
    }

    @Test
    void closedIsTerminal() {
        for (ClaimStatus target : ClaimStatus.values()) {
            assertFalse(ClaimStatus.CLOSED.canTransitionTo(target),
                "CLOSED must not transition to " + target);
        }
    }
}
