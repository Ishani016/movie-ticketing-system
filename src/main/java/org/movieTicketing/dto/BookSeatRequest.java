package org.movieTicketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookSeatRequest(
        @NotNull UUID screenId,
        @NotBlank String seatNumber,
        String userId
) {
}
