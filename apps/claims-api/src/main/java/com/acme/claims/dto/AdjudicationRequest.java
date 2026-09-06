package com.acme.claims.dto;

import jakarta.validation.constraints.Size;

public record AdjudicationRequest(
    @Size(max = 10_000) String notes
) {
}
