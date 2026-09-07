package com.acme.claims.api;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests against a real PostgreSQL 16. Skipped automatically
 * when no container runtime is available (e.g. local dev machines without
 * Docker); unit and state-machine tests still gate the build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("resource")
class ClaimApiIntegrationTest {

    /** The repo's canonical schema, resolved regardless of the surefire working directory. */
    private static final Path SCHEMA_FILE = findSchemaFile();

    private static Path findSchemaFile() {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path base : java.util.List.of(cwd, cwd.getParent(), cwd.getParent() == null ? cwd : cwd.getParent().getParent())) {
            if (base == null) {
                continue;
            }
            Path candidate = base.resolve("data").resolve("schemas").resolve("001_initial_schema.sql");
            if (java.nio.file.Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("001_initial_schema.sql not found upward from " + cwd);
    }

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    // Applied over JDBC instead of docker-entrypoint-initdb.d so the schema
    // source (data/schemas) stays the single source of truth and failures are
    // loud in the test output.
    static {
        POSTGRES.start();
        applySchema();
    }

    private static void applySchema() {
        String sql;
        try {
            sql = java.nio.file.Files.readString(SCHEMA_FILE);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + SCHEMA_FILE, e);
        }
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             java.sql.Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                String trimmed = part.strip();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Schema application failed", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private static final RequestPostProcessor ADJUSTER = jwt().jwt(jwt -> {
        jwt.subject("adjuster-1");
        jwt.issuer("http://localhost:8180/realms/acme-claims");
    });

    private static String createBody() {
        return """
            {
              "policyNumber": "POL-1000",
              "claimType": "AUTO",
              "incidentDate": "2026-09-01T10:00:00Z",
              "description": "Rear-end collision on I-95",
              "estimatedAmountMinor": 250000
            }
            """;
    }

    private static String claimId;

    @Test
    @Order(1)
    void createClaimIsIdempotent() throws Exception {
        String firstResponse = mockMvc.perform(post("/claims")
                .header("Idempotency-Key", "create-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FNOL"))
            .andExpect(jsonPath("$.claimNumber").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        claimId = com.fasterxml.jackson.databind.JsonNode.class.cast(
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(firstResponse)).get("id").asText();

        mockMvc.perform(post("/claims")
                .header("Idempotency-Key", "create-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Idempotency-Replayed", "true"))
            .andExpect(jsonPath("$.id").value(claimId));

        mockMvc.perform(post("/claims")
                .header("Idempotency-Key", "create-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody().replace("POL-1000", "POL-9999")))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/claims"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void invalidPayloadsAreRejected() throws Exception {
        mockMvc.perform(post("/claims")
                .header("Idempotency-Key", "invalid-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"claimType\": \"AUTO\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void illegalTransitionsAreRejected() throws Exception {
        mockMvc.perform(post("/claims/" + claimId + "/approve")
                .header("Idempotency-Key", "illegal-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approvedAmountMinor\": 100000}"))
            .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    void fullLifecycleReachesClosed() throws Exception {
        mockMvc.perform(post("/claims/" + claimId + "/triage")
                .header("Idempotency-Key", "triage-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\": 3, \"score\": 72.5, \"assignedTo\": \"adjuster-2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TRIAGE"));

        mockMvc.perform(post("/claims/" + claimId + "/adjudication")
                .header("Idempotency-Key", "adjudication-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\": \"Liability confirmed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ADJUDICATION"));

        mockMvc.perform(post("/claims/" + claimId + "/approve")
                .header("Idempotency-Key", "approve-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approvedAmountMinor\": 200000}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/claims/" + claimId + "/payout")
                .header("Idempotency-Key", "payout-1")
                .with(ADJUSTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"payoutReference\": \"PAY-2026-0001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAYOUT"));

        mockMvc.perform(post("/claims/" + claimId + "/payout/complete")
                .header("Idempotency-Key", "paid-1")
                .with(ADJUSTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"))
            .andExpect(jsonPath("$.paidAmountMinor").value(200000));

        mockMvc.perform(post("/claims/" + claimId + "/close")
                .header("Idempotency-Key", "close-1")
                .with(ADJUSTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @Order(6)
    void lookupByClaimNumberWorks() throws Exception {
        mockMvc.perform(get("/claims/" + claimId).with(ADJUSTER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.policyNumber").value("POL-1000"));

        String claimNumber = mockMvc.perform(get("/claims/" + claimId).with(ADJUSTER))
            .andReturn().getResponse().getContentAsString();

        String number = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(claimNumber).get("claimNumber").asText();

        mockMvc.perform(get("/claims/number/" + number).with(SecurityMockMvcRequestPostProcessors.jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(claimId));
    }
}
