package com.acme.claims.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Approval decision payload. The approved amount is in minor units (ADR-007)
 * and may be less than or equal to the estimate; the pack does not set a
 * tolerance rule, so none is enforced.
 */
public record ApprovalRequest(
    @NotNull @PositiveOrZero Long approvedAmountMinor
) {
}
