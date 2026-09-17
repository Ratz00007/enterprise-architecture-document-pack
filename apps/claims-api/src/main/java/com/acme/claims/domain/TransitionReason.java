package com.acme.claims.domain;

import java.util.UUID;

/**
 * Why a claim transitioned. Carried in the same transaction as
 * the state change. Written verbatim to {@code claim_state_history}.
 */
public record TransitionReason(
        UUID actorUserId,
        String comment,
        UUID evidenceItemId,
        String genaiSummary
) {
    public TransitionReason {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("transition reason must have a non-blank comment");
        }
    }
}
