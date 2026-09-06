package com.acme.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PayoutRequest(
    @NotBlank @Size(max = 100) String payoutReference
) {
}
