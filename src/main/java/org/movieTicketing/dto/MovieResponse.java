package org.movieTicketing.dto;

import org.movieTicketing.models.Genre;
import org.movieTicketing.models.Movie;

import java.util.UUID;

public record MovieResponse(
        UUID id,
        String name,
        Genre genre,
        Long durationInMinutes
) {
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getName(), movie.getGenre(), movie.getDurationInMinutes());
    }
}
