package com.acme.claims.domain;

import java.util.Set;

/**
 * The lifecycle states of a claim. See
 * {@code docs/domain/workflows.md} for the state machine and the
 * valid transitions.
 *
 * <p>This is a sealed interface — every legal state is enumerated
 * at compile time. Illegal state transitions are caught by
 * {@link Claim#transitionTo(ClaimState, TransitionReason)}.
 */
public sealed interface ClaimState
        permits ClaimState.New,
                ClaimState.Triage,
                ClaimState.Investigating,
                ClaimState.DecisionPending,
                ClaimState.Approved,
                ClaimState.Reserved,
                ClaimState.PayoutPending,
                ClaimState.Paid,
                ClaimState.Denied,
                ClaimState.Withdrawn,
                ClaimState.Closed,
                ClaimState.Archived,
                ClaimState.Reopened {

    /** The terminal "you can be reopened" set. */
    Set<ClaimState> REOPENABLE_FROM = Set.of(Closed.instance);

    /** The valid forward transitions. */
    Set<ClaimState> canTransitionTo(ClaimState self);

    record New() implements ClaimState {
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Triage.instance);
        }
    }
    record Triage() implements ClaimState {
        public static final Triage instance = new Triage();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Investigating.instance);
        }
    }
    record Investigating() implements ClaimState {
        public static final Investigating instance = new Investigating();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(DecisionPending.instance);
        }
    }
    record DecisionPending() implements ClaimState {
        public static final DecisionPending instance = new DecisionPending();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Approved.instance, Denied.instance, Withdrawn.instance);
        }
    }
    record Approved() implements ClaimState {
        public static final Approved instance = new Approved();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Reserved.instance);
        }
    }
    record Reserved() implements ClaimState {
        public static final Reserved instance = new Reserved();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(PayoutPending.instance);
        }
    }
    record PayoutPending() implements ClaimState {
        public static final PayoutPending instance = new PayoutPending();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Paid.instance);
        }
    }
    record Paid() implements ClaimState {
        public static final Paid instance = new Paid();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Closed.instance);
        }
    }
    record Denied() implements ClaimState {
        public static final Denied instance = new Denied();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Closed.instance);
        }
    }
    record Withdrawn() implements ClaimState {
        public static final Withdrawn instance = new Withdrawn();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Closed.instance);
        }
    }
    record Closed() implements ClaimState {
        public static final Closed instance = new Closed();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Archived.instance, Reopened.instance);
        }
    }
    record Archived() implements ClaimState {
        public static final Archived instance = new Archived();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of();
        }
    }
    record Reopened() implements ClaimState {
        public static final Reopened instance = new Reopened();
        @Override public Set<ClaimState> canTransitionTo(ClaimState self) {
            return Set.of(Investigating.instance);
        }
    }
}
