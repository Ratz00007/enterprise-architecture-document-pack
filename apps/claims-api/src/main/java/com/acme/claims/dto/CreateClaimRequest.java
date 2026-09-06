package com.acme.claims.dto;

import com.acme.claims.domain.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * FNOL submission payload. Amounts are integer minor units (ADR-007).
 */
public record CreateClaimRequest(
    @NotBlank @Size(max = 50) String policyNumber,
    @NotNull ClaimType claimType,
    @NotNull Instant incidentDate,
    @Size(max = 10_000) String description,
    @PositiveOrZero Long estimatedAmountMinor
) {
}
