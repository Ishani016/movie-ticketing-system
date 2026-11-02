package org.movieTicketing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotBlank String customerName,
        @NotBlank @Email String customerEmail,
        @NotEmpty @Valid List<BookedSeatRef> tickets
) {
    public record BookedSeatRef(
            @NotNull UUID screenId,
            @NotBlank String seatNumber
    ) {
    }
}
