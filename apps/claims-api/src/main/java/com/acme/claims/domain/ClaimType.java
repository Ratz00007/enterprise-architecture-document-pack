package com.acme.claims.domain;

/**
 * Line-of-business classification for a claim. The baseline enumeration is
 * carried over from the merged skeleton; the pack does not enumerate claim
 * types, so this list must not grow without an ADR.
 */
public enum ClaimType {
    AUTO,
    PROPERTY,
    HEALTH,
    LIFE,
    LIABILITY,
    WORKERS_COMP,
    OTHER
}
