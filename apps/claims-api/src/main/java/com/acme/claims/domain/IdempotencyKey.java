package com.acme.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Stored result of a request processed under an {@code Idempotency-Key} header
 * (ADR-008). A client that retries the same key on the same endpoint with the
 * same request body receives the stored response instead of a duplicate
 * execution.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 255)
    private String key;

    @Column(nullable = false, updatable = false, length = 255)
    private String endpoint;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyKey(String key, String endpoint, String requestHash, int responseStatus, String responseBody) {
        this.key = key;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    protected IdempotencyKey() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getKey() {
        return key;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
