package org.movieTicketing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoomRequest(
        @NotBlank String name,
        @NotNull @Min(1) Integer rows,
        @NotNull @Min(1) Integer columns,
        List<Integer> premiumRows,
        List<Integer> luxeRows
) {
}
