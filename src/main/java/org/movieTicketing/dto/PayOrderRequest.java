package org.movieTicketing.dto;

import jakarta.validation.constraints.NotNull;

public record PayOrderRequest(
        @NotNull Boolean success
) {
}
