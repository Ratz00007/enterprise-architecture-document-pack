package com.acme.claims.service;

import com.acme.claims.domain.IdempotencyConflictException;
import com.acme.claims.domain.IdempotencyKey;
import com.acme.claims.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyService service;

    @Test
    void hashOfIsDeterministicAndSensitiveToContent() {
        String first = service.hashOf(new Payload("a"));
        String second = service.hashOf(new Payload("a"));
        String other = service.hashOf(new Payload("b"));

        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(other).isNotEqualTo(first);
    }

    @Test
    void replayReturnsStoredResponseWhenHashMatches() {
        IdempotencyKey stored = new IdempotencyKey("key-1", "POST /claims", "hash-1", 201, "{\"ok\":true}");
        when(repository.findByKeyAndEndpoint("key-1", "POST /claims")).thenReturn(Optional.of(stored));

        Optional<IdempotencyService.Replay> replay = service.replayFor("key-1", "POST /claims", "hash-1");

        assertThat(replay).contains(new IdempotencyService.Replay(201, "{\"ok\":true}"));
    }

    @Test
    void replayIsEmptyForUnknownKey() {
        when(repository.findByKeyAndEndpoint(any(), any())).thenReturn(Optional.empty());

        assertThat(service.replayFor("key-x", "POST /claims", "hash-x")).isEmpty();
    }

    @Test
    void sameKeyWithDifferentPayloadIsAConflict() {
        IdempotencyKey stored = new IdempotencyKey("key-1", "POST /claims", "hash-1", 201, "{\"ok\":true}");
        when(repository.findByKeyAndEndpoint("key-1", "POST /claims")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.replayFor("key-1", "POST /claims", "hash-other"))
            .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void recordSucceedsOnFreshKey() {
        assertThatCode(() -> service.record("key-2", "POST /claims", "hash-2", 201, "{}"))
            .doesNotThrowAnyException();
    }

    @Test
    void recordReportsConflictOnDuplicateKey() {
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.record("key-2", "POST /claims", "hash-2", 201, "{}"))
            .isInstanceOf(IdempotencyConflictException.class);
    }

    private record Payload(String value) {
    }
}
