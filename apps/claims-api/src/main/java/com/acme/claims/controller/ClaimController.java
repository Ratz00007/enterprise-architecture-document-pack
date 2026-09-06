package com.acme.claims.controller;

import com.acme.claims.model.Claim;
import com.acme.claims.service.ClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Claim Controller - REST API endpoints for claim management
 */
@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public ResponseEntity<Page<Claim>> getAllClaims(Pageable pageable) {
        return ResponseEntity.ok(claimService.getAllClaims(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaimById(@PathVariable UUID id) {
        return claimService.getClaimById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Claim> createClaim(
            @RequestBody Claim claim,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Claim createdClaim = claimService.createClaim(claim, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClaim);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Claim> updateClaim(
            @PathVariable UUID id,
            @RequestBody Claim updatedClaim,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            Claim claim = claimService.updateClaim(id, updatedClaim, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/triage")
    public ResponseEntity<Claim> transitionToTriage(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> triageData,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        int priority = (Integer) triageData.get("priority");
        Double score = ((Number) triageData.get("score")).doubleValue();
        try {
            Claim claim = claimService.transitionToTriage(id, priority, score, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/adjudication")
    public ResponseEntity<Claim> transitionToAdjudication(
            @PathVariable UUID id,
            @RequestBody Map<String, String> adjudicationData,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String notes = adjudicationData.get("notes");
        try {
            Claim claim = claimService.transitionToAdjudication(id, notes, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Claim> approveClaim(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> approvalData,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        BigDecimal approvedAmount = approvalData.get("approvedAmount");
        try {
            Claim claim = claimService.approveClaim(id, approvedAmount, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Claim> rejectClaim(
            @PathVariable UUID id,
            @RequestBody Map<String, String> rejectionData,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String reason = rejectionData.get("reason");
        try {
            Claim claim = claimService.rejectClaim(id, reason, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/payout/process")
    public ResponseEntity<Claim> processPayout(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payoutData,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String payoutReference = payoutData.get("payoutReference");
        try {
            Claim claim = claimService.processPayout(id, payoutReference, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/payout/complete")
    public ResponseEntity<Claim> completePayout(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            Claim claim = claimService.completePayout(id, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Claim> closeClaim(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            Claim claim = claimService.closeClaim(id, userId);
            return ResponseEntity.ok(claim);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Claim>> getClaimsByStatus(
            @PathVariable Claim.ClaimStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(claimService.getClaimsByStatus(status, pageable));
    }

    @GetMapping("/high-priority")
    public ResponseEntity<List<Claim>> getHighPriorityClaims(
            @RequestParam Claim.ClaimStatus status,
            @RequestParam int minPriority) {
        return ResponseEntity.ok(claimService.getHighPriorityClaims(status, minPriority));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable UUID id) {
        try {
            claimService.deleteClaim(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
