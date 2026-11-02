package org.movieTicketing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateScreeningRequest(
        @NotNull UUID movieId,
        @NotNull UUID cinemaId,
        @NotNull UUID roomId,
        @NotNull LocalDateTime startTime,
        LocalDateTime endTime,
        @NotNull @Positive BigDecimal basePrice
) {
}
