package com.acme.claims.controller;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimNotFoundException;
import com.acme.claims.domain.ClaimStatus;
import com.acme.claims.domain.ClaimType;
import com.acme.claims.dto.ClaimResponse;
import com.acme.claims.dto.CreateClaimRequest;
import com.acme.claims.dto.TriageRequest;
import com.acme.claims.service.ClaimService;
import com.acme.claims.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService claimService;

    @Mock
    private IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private ClaimController controller;

    private Jwt jwt;

    @BeforeEach
    void setUp() {
        controller = new ClaimController(claimService, idempotencyService, objectMapper);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject("adjuster-1").build();
    }

    private static Claim claimIn(ClaimStatus status) {
        Claim claim = new Claim();
        claim.setClaimNumber("CLM-20260906-ABC123");
        claim.setPolicyNumber("POL-1000");
        claim.setClaimType(ClaimType.AUTO);
        claim.setIncidentDate(Instant.parse("2026-09-01T10:00:00Z"));
        claim.setReportedDate(Instant.now());
        claim.setStatus(status);
        return claim;
    }

    @Test
    void createClaimReturnsCreatedAndRecordsIdempotency() {
        Claim claim = claimIn(ClaimStatus.FNOL);
        when(idempotencyService.hashOf(any())).thenReturn("hash-1");
        when(idempotencyService.replayFor("key-1", "POST /claims", "hash-1")).thenReturn(Optional.empty());
        when(claimService.createClaim(any(CreateClaimRequest.class), eq("adjuster-1"))).thenReturn(claim);

        ResponseEntity<ClaimResponse> response = controller.createClaim(
            new CreateClaimRequest("POL-1000", ClaimType.AUTO, Instant.now(), "desc", 1L),
            "key-1", jwt, new MockHttpServletRequest("POST", "/claims"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().claimNumber()).isEqualTo("CLM-20260906-ABC123");
        verify(idempotencyService).record(eq("key-1"), eq("POST /claims"), eq("hash-1"), eq(201), anyString());
    }

    @Test
    void createClaimReplaysStoredResponseForSameKey() throws Exception {
        Claim claim = claimIn(ClaimStatus.FNOL);
        String storedJson = objectMapper.writeValueAsString(ClaimResponse.from(claim));
        when(idempotencyService.hashOf(any())).thenReturn("hash-1");
        when(idempotencyService.replayFor("key-1", "POST /claims", "hash-1"))
            .thenReturn(Optional.of(new IdempotencyService.Replay(201, storedJson)));

        ResponseEntity<ClaimResponse> response = controller.createClaim(
            new CreateClaimRequest("POL-1000", ClaimType.AUTO, Instant.now(), "desc", 1L),
            "key-1", jwt, new MockHttpServletRequest("POST", "/claims"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst(ClaimController.IDEMPOTENCY_REPLAYED_HEADER)).isEqualTo("true");
        assertThat(response.getBody().claimNumber()).isEqualTo("CLM-20260906-ABC123");
        verify(claimService, never()).createClaim(any(), anyString());
    }

    @Test
    void transitionEndpointDelegatesAndStoresIdempotency() {
        Claim claim = claimIn(ClaimStatus.TRIAGE);
        when(idempotencyService.hashOf(any())).thenReturn("hash-2");
        when(idempotencyService.replayFor(eq("key-2"), anyString(), anyString())).thenReturn(Optional.empty());
        when(claimService.triage(eq(claim.getId()), any(), eq("adjuster-1"))).thenReturn(claim);

        ResponseEntity<ClaimResponse> response = controller.triage(claim.getId(),
            new TriageRequest(2, 50.0, "adjuster-2"), "key-2", jwt,
            new MockHttpServletRequest("POST", "/claims/" + claim.getId() + "/triage"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(ClaimStatus.TRIAGE);
    }

    @Test
    void bodylessTransitionsWork() {
        Claim claim = claimIn(ClaimStatus.PAID);
        claim.setApprovedAmountMinor(1L);
        when(idempotencyService.hashOf(any())).thenReturn("hash-3");
        when(idempotencyService.replayFor(eq("key-3"), anyString(), anyString())).thenReturn(Optional.empty());
        when(claimService.completePayout(eq(claim.getId()), anyString())).thenReturn(claim);

        ResponseEntity<ClaimResponse> response = controller.completePayout(claim.getId(), "key-3", jwt,
            new MockHttpServletRequest("POST", "/claims/" + claim.getId() + "/payout/complete"));

        assertThat(response.getBody().status()).isEqualTo(ClaimStatus.PAID);
    }

    @Test
    void lookupEndpointsDelegate() {
        UUID id = UUID.randomUUID();
        Claim claim = claimIn(ClaimStatus.FNOL);
        when(claimService.getById(id)).thenReturn(claim);
        when(claimService.getByClaimNumber("CLM-20260906-ABC123")).thenReturn(claim);
        when(claimService.list(eq(ClaimStatus.FNOL), any())).thenReturn(Page.empty());

        assertThat(controller.getClaim(id).claimNumber()).isEqualTo("CLM-20260906-ABC123");
        assertThat(controller.getClaimByNumber("CLM-20260906-ABC123").policyNumber()).isEqualTo("POL-1000");
        assertThat(controller.listClaims(ClaimStatus.FNOL, PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @Test
    void missingClaimSurfacesAsNotFound() {
        UUID id = UUID.randomUUID();
        when(claimService.getById(id)).thenThrow(new ClaimNotFoundException(id));

        assertThatThrownBy(() -> controller.getClaim(id)).isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void remainingTransitionEndpointsDelegate() {
        Claim claim = claimIn(ClaimStatus.CLOSED);
        when(idempotencyService.hashOf(any())).thenReturn("hash-n");
        when(idempotencyService.replayFor(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(claimService.beginAdjudication(eq(claim.getId()), any(), anyString())).thenReturn(claim);
        when(claimService.approve(eq(claim.getId()), any(), anyString())).thenReturn(claim);
        when(claimService.reject(eq(claim.getId()), any(), anyString())).thenReturn(claim);
        when(claimService.payout(eq(claim.getId()), any(), anyString())).thenReturn(claim);
        when(claimService.close(eq(claim.getId()), anyString())).thenReturn(claim);

        String uri = "/claims/" + claim.getId();
        assertThat(controller.beginAdjudication(claim.getId(),
            new com.acme.claims.dto.AdjudicationRequest("notes"), "k1", jwt,
            new MockHttpServletRequest("POST", uri + "/adjudication")).getBody().status())
            .isEqualTo(ClaimStatus.CLOSED);
        assertThat(controller.approve(claim.getId(),
            new com.acme.claims.dto.ApprovalRequest(1L), "k2", jwt,
            new MockHttpServletRequest("POST", uri + "/approve")).getBody().status())
            .isEqualTo(ClaimStatus.CLOSED);
        assertThat(controller.reject(claim.getId(),
            new com.acme.claims.dto.RejectionRequest("reason"), "k3", jwt,
            new MockHttpServletRequest("POST", uri + "/reject")).getBody().status())
            .isEqualTo(ClaimStatus.CLOSED);
        assertThat(controller.payout(claim.getId(),
            new com.acme.claims.dto.PayoutRequest("PAY-1"), "k4", jwt,
            new MockHttpServletRequest("POST", uri + "/payout")).getBody().status())
            .isEqualTo(ClaimStatus.CLOSED);
        assertThat(controller.close(claim.getId(), "k5", jwt,
            new MockHttpServletRequest("POST", uri + "/close")).getBody().status())
            .isEqualTo(ClaimStatus.CLOSED);
    }
}
