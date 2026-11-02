package org.movieTicketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LocationRequest(
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        @NotBlank String address
) {
}
