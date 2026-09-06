package com.acme.claims.domain;

import java.util.UUID;

/**
 * Thrown when a referenced claim does not exist. Maps to HTTP 404.
 */
public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(UUID id) {
        super("Claim not found: %s".formatted(id));
    }

    public ClaimNotFoundException(String claimNumber) {
        super("Claim not found: %s".formatted(claimNumber));
    }
}
