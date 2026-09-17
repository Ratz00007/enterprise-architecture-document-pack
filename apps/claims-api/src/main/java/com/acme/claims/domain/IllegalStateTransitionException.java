package com.acme.claims.domain;

/**
 * Thrown when a code path attempts an illegal state transition
 * on a {@link Claim}. The API layer maps this to HTTP 409 Conflict
 * with a structured error body.
 */
public final class IllegalStateTransitionException extends RuntimeException {

    private final ClaimState from;
    private final ClaimState to;

    public IllegalStateTransitionException(ClaimState from, ClaimState to, TransitionReason reason) {
        super("Illegal state transition for claim: " + from + " -> " + to
                + " (reason: " + (reason == null ? "<none>" : reason.comment()) + ")");
        this.from = from;
        this.to = to;
    }

    public ClaimState from() { return from; }
    public ClaimState to() { return to; }
}
