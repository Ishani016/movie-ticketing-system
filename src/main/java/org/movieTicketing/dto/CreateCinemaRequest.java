package org.movieTicketing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCinemaRequest(
        @NotBlank String name,
        @NotNull @Valid LocationRequest location,
        @NotEmpty @Valid List<CreateRoomRequest> rooms
) {
}
