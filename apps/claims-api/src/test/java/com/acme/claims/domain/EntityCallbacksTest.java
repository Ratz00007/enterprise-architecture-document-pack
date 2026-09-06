package com.acme.claims.domain;

import com.acme.claims.util.UuidV7;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the JPA entity callbacks and accessors directly; the container
 * integration suite covers them through Hibernate as well.
 */
class EntityCallbacksTest {

    @Test
    void claimPrePersistAssignsUuidV7AndCreatedAt() {
        Claim claim = new Claim();

        claim.onCreate();

        assertThat(claim.getId()).isNotNull();
        assertThat(claim.getId().version()).isEqualTo(7);
        assertThat(claim.getCreatedAt()).isNotNull();
    }

    @Test
    void claimPrePersistKeepsExistingIdentityAndTimestamp() {
        Claim claim = new Claim();
        UUID fixed = UuidV7.randomUUID();
        Instant fixedCreatedAt = Instant.parse("2026-09-06T00:00:00Z");
        ReflectionTestUtils.setField(claim, "id", fixed);
        ReflectionTestUtils.setField(claim, "createdAt", fixedCreatedAt);

        claim.onCreate();

        assertThat(claim.getId()).isEqualTo(fixed);
        assertThat(claim.getCreatedAt()).isEqualTo(fixedCreatedAt);
    }

    @Test
    void claimPreUpdateStampsUpdatedAt() {
        Claim claim = new Claim();
        assertThat(claim.getUpdatedAt()).isNull();

        claim.onUpdate();

        assertThat(claim.getUpdatedAt()).isNotNull();
    }

    @Test
    void transitionToAcceptsLegalAndRejectsIllegal() {
        Claim claim = new Claim();
        claim.setStatus(ClaimStatus.FNOL);

        claim.transitionTo(ClaimStatus.TRIAGE);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.TRIAGE);
        assertThatThrownBy(() -> claim.transitionTo(ClaimStatus.CLOSED))
            .isInstanceOf(InvalidClaimTransitionException.class)
            .hasMessageContaining("TRIAGE")
            .hasMessageContaining("CLOSED")
            .satisfies(e -> {
                InvalidClaimTransitionException ex = (InvalidClaimTransitionException) e;
                assertThat(ex.getFrom()).isEqualTo(ClaimStatus.TRIAGE);
                assertThat(ex.getTo()).isEqualTo(ClaimStatus.CLOSED);
            });
    }

    @Test
    void idempotencyKeyCallbacksAndAccessors() {
        IdempotencyKey generated = new IdempotencyKey();
        generated.onCreate();
        assertThat(generated.getCreatedAt()).isNotNull();

        Instant fixed = Instant.parse("2026-09-06T00:00:00Z");
        IdempotencyKey key = new IdempotencyKey("key-1", "POST /claims", "hash", 201, "{}");
        ReflectionTestUtils.setField(key, "createdAt", fixed);
        key.onCreate();
        assertThat(key.getCreatedAt()).isEqualTo(fixed);

        assertThat(key.getKey()).isEqualTo("key-1");
        assertThat(key.getEndpoint()).isEqualTo("POST /claims");
        assertThat(key.getRequestHash()).isEqualTo("hash");
        assertThat(key.getResponseStatus()).isEqualTo(201);
        assertThat(key.getResponseBody()).isEqualTo("{}");
    }
}
