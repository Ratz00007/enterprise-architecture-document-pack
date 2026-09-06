package com.acme.claims.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * RFC 9562 UUIDv7 generator (ADR-009). Primary keys are generated
 * application-side so they are time-ordered without a database extension.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERSION_7_MASK = 0x7000L;
    private static final long VARIANT_MASK = 0x8000000000000000L;
    private static final long VARIANT_RANDOM_BITS = 0x3FFFFFFFFFFFFFFFL;

    private UuidV7() {
    }

    public static UUID randomUUID() {
        long unixMillis = System.currentTimeMillis();
        long randA = RANDOM.nextLong(0x1000); // 12 bits
        long randB = RANDOM.nextLong(1L << 62);

        long mostSigBits = (unixMillis & 0xFFFFFFFFFFFFL) << 16;
        mostSigBits |= VERSION_7_MASK;
        mostSigBits |= randA;

        long leastSigBits = VARIANT_MASK | (randB & VARIANT_RANDOM_BITS);

        return new UUID(mostSigBits, leastSigBits);
    }
}
