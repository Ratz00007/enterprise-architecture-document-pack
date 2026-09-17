package com.acme.claims.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Pure domain — no Spring, no JPA annotations.
 * The infrastructure layer maps this to a JPA entity; the API
 * layer maps it to a DTO. The application layer is the only
 * place that touches both sides.
 *
 * <p>State transitions are append-only. Every change to
 * {@code state} is also written to {@code ClaimStateHistory} in
 * the same transaction.
 */
public final class Claim {

    private final UUID id;
    private final String claimNumber;
    private final UUID policyId;
    private final UUID claimantPartyId;
    private final ClaimState state;
    private final Instant lossDate;
    private final Instant reportedDate;
    private final long estimatedAmountCents;
    private final String currency;
    private final long version;

    private Claim(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.claimNumber = Objects.requireNonNull(b.claimNumber, "claimNumber");
        this.policyId = Objects.requireNonNull(b.policyId, "policyId");
        this.claimantPartyId = Objects.requireNonNull(b.claimantPartyId, "claimantPartyId");
        this.state = Objects.requireNonNull(b.state, "state");
        this.lossDate = Objects.requireNonNull(b.lossDate, "lossDate");
        this.reportedDate = Objects.requireNonNull(b.reportedDate, "reportedDate");
        this.estimatedAmountCents = b.estimatedAmountCents;
        this.currency = Objects.requireNonNull(b.currency, "currency");
        this.version = b.version;
    }

    public Claim transitionTo(ClaimState next, TransitionReason reason) {
        if (!state.canTransitionTo(state).contains(next)) {
            throw new IllegalStateTransitionException(state, next, reason);
        }
        return toBuilder().state(next).version(version + 1).build();
    }

    public UUID id() { return id; }
    public String claimNumber() { return claimNumber; }
    public UUID policyId() { return policyId; }
    public UUID claimantPartyId() { return claimantPartyId; }
    public ClaimState state() { return state; }
    public Instant lossDate() { return lossDate; }
    public Instant reportedDate() { return reportedDate; }
    public long estimatedAmountCents() { return estimatedAmountCents; }
    public String currency() { return currency; }
    public long version() { return version; }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .claimNumber(claimNumber)
                .policyId(policyId)
                .claimantPartyId(claimantPartyId)
                .state(state)
                .lossDate(lossDate)
                .reportedDate(reportedDate)
                .estimatedAmountCents(estimatedAmountCents)
                .currency(currency)
                .version(version);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String claimNumber;
        private UUID policyId;
        private UUID claimantPartyId;
        private ClaimState state;
        private Instant lossDate;
        private Instant reportedDate;
        private long estimatedAmountCents;
        private String currency;
        private long version;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder claimNumber(String v) { this.claimNumber = v; return this; }
        public Builder policyId(UUID v) { this.policyId = v; return this; }
        public Builder claimantPartyId(UUID v) { this.claimantPartyId = v; return this; }
        public Builder state(ClaimState v) { this.state = v; return this; }
        public Builder lossDate(Instant v) { this.lossDate = v; return this; }
        public Builder reportedDate(Instant v) { this.reportedDate = v; return this; }
        public Builder estimatedAmountCents(long v) { this.estimatedAmountCents = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder version(long v) { this.version = v; return this; }

        public Claim build() { return new Claim(this); }
    }
}
