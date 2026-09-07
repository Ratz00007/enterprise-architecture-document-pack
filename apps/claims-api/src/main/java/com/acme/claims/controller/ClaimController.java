package com.acme.claims.controller;

import com.acme.claims.domain.Claim;
import com.acme.claims.domain.ClaimStatus;
import com.acme.claims.dto.AdjudicationRequest;
import com.acme.claims.dto.ApprovalRequest;
import com.acme.claims.dto.ClaimResponse;
import com.acme.claims.dto.CreateClaimRequest;
import com.acme.claims.dto.PayoutRequest;
import com.acme.claims.dto.RejectionRequest;
import com.acme.claims.dto.TriageRequest;
import com.acme.claims.service.ClaimService;
import com.acme.claims.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Claims REST API. All state-mutating endpoints require an
 * {@code Idempotency-Key} header (ADR-008); replays return the stored
 * response with an {@code Idempotency-Replayed: true} header.
 */
@RestController
@RequestMapping("/claims")
public class ClaimController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_REPLAYED_HEADER = "Idempotency-Replayed";

    private final ClaimService claimService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public ClaimController(ClaimService claimService,
                           IdempotencyService idempotencyService,
                           ObjectMapper objectMapper) {
        this.claimService = claimService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(
        @Valid @RequestBody CreateClaimRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 201, () -> {
            Claim claim = claimService.createClaim(request, jwt.getSubject());
            return ClaimResponse.from(claim);
        });
    }

    @GetMapping
    public Page<ClaimResponse> listClaims(
        @RequestParam(required = false) ClaimStatus status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return claimService.list(status, pageable).map(ClaimResponse::from);
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaim(@PathVariable UUID id) {
        return ClaimResponse.from(claimService.getById(id));
    }

    @GetMapping("/number/{claimNumber}")
    public ClaimResponse getClaimByNumber(@PathVariable String claimNumber) {
        return ClaimResponse.from(claimService.getByClaimNumber(claimNumber));
    }

    @PostMapping("/{id}/triage")
    public ResponseEntity<ClaimResponse> triage(
        @PathVariable UUID id,
        @Valid @RequestBody TriageRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 200,
            () -> ClaimResponse.from(claimService.triage(id, request, jwt.getSubject())));
    }

    @PostMapping("/{id}/adjudication")
    public ResponseEntity<ClaimResponse> beginAdjudication(
        @PathVariable UUID id,
        @Valid @RequestBody AdjudicationRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 200,
            () -> ClaimResponse.from(claimService.beginAdjudication(id, request, jwt.getSubject())));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ClaimResponse> approve(
        @PathVariable UUID id,
        @Valid @RequestBody ApprovalRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 200,
            () -> ClaimResponse.from(claimService.approve(id, request, jwt.getSubject())));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ClaimResponse> reject(
        @PathVariable UUID id,
        @Valid @RequestBody RejectionRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 200,
            () -> ClaimResponse.from(claimService.reject(id, request, jwt.getSubject())));
    }

    @PostMapping("/{id}/payout")
    public ResponseEntity<ClaimResponse> payout(
        @PathVariable UUID id,
        @Valid @RequestBody PayoutRequest request,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, request, 200,
            () -> ClaimResponse.from(claimService.payout(id, request, jwt.getSubject())));
    }

    @PostMapping("/{id}/payout/complete")
    public ResponseEntity<ClaimResponse> completePayout(
        @PathVariable UUID id,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, "", 200,
            () -> ClaimResponse.from(claimService.completePayout(id, jwt.getSubject())));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ClaimResponse> close(
        @PathVariable UUID id,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest http
    ) {
        return replayOrExecute(idempotencyKey, http, "", 200,
            () -> ClaimResponse.from(claimService.close(id, jwt.getSubject())));
    }

    private ResponseEntity<ClaimResponse> replayOrExecute(
        String idempotencyKey,
        HttpServletRequest http,
        Object requestBody,
        int successStatus,
        Supplier<ClaimResponse> action
    ) {
        String endpoint = http.getMethod() + " " + http.getRequestURI();
        String requestHash = idempotencyService.hashOf(requestBody);

        return idempotencyService.replayFor(idempotencyKey, endpoint, requestHash)
            .<ResponseEntity<ClaimResponse>>map(replay -> ResponseEntity.status(replay.status())
                .header(IDEMPOTENCY_REPLAYED_HEADER, "true")
                .body(readResponse(replay.body())))
            .orElseGet(() -> {
                ClaimResponse response = action.get();
                idempotencyService.record(idempotencyKey, endpoint, requestHash, successStatus,
                    writeResponse(response));
                return ResponseEntity.status(successStatus).body(response);
            });
    }

    private String writeResponse(ClaimResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new IllegalStateException("Response is not serializable", e);
        }
    }

    private ClaimResponse readResponse(String json) {
        try {
            return objectMapper.readValue(json, ClaimResponse.class);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new IllegalStateException("Stored idempotency response is not deserializable", e);
        }
    }
}
