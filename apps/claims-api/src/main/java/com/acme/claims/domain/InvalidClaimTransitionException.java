package com.acme.claims.domain;

/**
 * Thrown when a claim state change is not permitted by the lifecycle in
 * {@link ClaimStatus}. Maps to HTTP 409.
 */
public class InvalidClaimTransitionException extends RuntimeException {

    private final ClaimStatus from;
    private final ClaimStatus to;

    public InvalidClaimTransitionException(ClaimStatus from, ClaimStatus to) {
        super("Claim cannot transition from %s to %s".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    public ClaimStatus getFrom() {
        return from;
    }

    public ClaimStatus getTo() {
        return to;
    }
}
