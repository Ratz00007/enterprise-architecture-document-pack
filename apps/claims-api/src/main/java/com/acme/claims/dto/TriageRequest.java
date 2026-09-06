package com.acme.claims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TriageRequest(
    @NotNull @Min(1) @Max(5) Integer priority,
    @NotNull @Min(0) @Max(100) Double score,
    @Size(max = 100) String assignedTo
) {
}
