package com.acme.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectionRequest(
    @NotBlank @Size(max = 10_000) String reason
) {
}
