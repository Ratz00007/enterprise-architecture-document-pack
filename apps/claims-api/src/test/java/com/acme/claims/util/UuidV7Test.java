package com.acme.claims.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidV7Test {

    @Test
    void setsVersionAndVariantBits() {
        for (int i = 0; i < 1_000; i++) {
            UUID uuid = UuidV7.randomUUID();
            assertEquals(7, (uuid.getMostSignificantBits() >>> 12) & 0xF, "version nibble must be 7");
            assertEquals(0x8000000000000000L, uuid.getLeastSignificantBits() & 0xC000000000000000L,
                "variant bits must be 10x per RFC 9562");
        }
    }

    @Test
    void encodesCurrentUnixTimestamp() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidV7.randomUUID();
        long after = System.currentTimeMillis();

        long encoded = uuid.getMostSignificantBits() >>> 16;
        assertTrue(encoded >= before - 1 && encoded <= after,
            "embedded timestamp %d must be within [%d, %d]".formatted(encoded, before, after));
    }

    @Test
    void ordersByTime() throws InterruptedException {
        UUID first = UuidV7.randomUUID();
        Thread.sleep(5);
        UUID second = UuidV7.randomUUID();

        assertTrue(Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits()) < 0,
            "later UUIDv7 must sort after an earlier one");
    }

    @Test
    void generatesUniqueValues() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(UuidV7.randomUUID());
        }
        assertEquals(10_000, seen.size());
    }
}
