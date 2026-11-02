package org.movieTicketing.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Movie {
    @EqualsAndHashCode.Include
    private final UUID id;
    private String name;
    private Genre genre;
    private Long durationInMinutes;

    public Movie(String name, Genre genre, Long durationInMinutes) {
        this(UUID.randomUUID(), name, genre, durationInMinutes);
    }

    public Movie(UUID id, String name, Genre genre, Long durationInMinutes) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.durationInMinutes = durationInMinutes;
    }
}
