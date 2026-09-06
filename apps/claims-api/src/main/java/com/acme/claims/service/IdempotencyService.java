package com.acme.claims.service;

import com.acme.claims.domain.IdempotencyConflictException;
import com.acme.claims.domain.IdempotencyKey;
import com.acme.claims.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Stores and replays responses for requests carrying an {@code Idempotency-Key}
 * (ADR-008). A stored entry is replayed only when both the endpoint and the
 * request-body hash match; otherwise the reuse is a conflict.
 */
@Service
public class IdempotencyService {

    public record Replay(int status, String body) {
    }

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String hashOf(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new IllegalArgumentException("Request body is not serializable", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Replay> replayFor(String key, String endpoint, String requestHash) {
        return repository.findByKeyAndEndpoint(key, endpoint)
            .map(stored -> {
                if (!stored.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(key);
                }
                log.debug("Replaying stored response for idempotency key {} on {}", key, endpoint);
                return new Replay(stored.getResponseStatus(), stored.getResponseBody());
            });
    }

    @Transactional
    public void record(String key, String endpoint, String requestHash, int status, String responseBody) {
        try {
            repository.saveAndFlush(new IdempotencyKey(key, endpoint, requestHash, status, responseBody));
        } catch (DataIntegrityViolationException e) {
            throw new IdempotencyConflictException(key);
        }
    }
}
