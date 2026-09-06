package com.acme.claims.domain;

/**
 * Thrown when an {@code Idempotency-Key} is reused with a different request
 * payload, or when a concurrent request with the same key is in flight. Maps
 * to HTTP 409 (ADR-008).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency key %s was already used with a different request".formatted(key));
    }
}
