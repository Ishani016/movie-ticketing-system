package org.movieTicketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.movieTicketing.models.Genre;

public record CreateMovieRequest(
        @NotBlank String name,
        @NotNull Genre genre,
        @NotNull @Positive Long durationInMinutes
) {
}
