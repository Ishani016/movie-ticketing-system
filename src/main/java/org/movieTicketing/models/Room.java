package org.movieTicketing.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Room {
    @EqualsAndHashCode.Include
    private final UUID id;
    private String name;
    @ToString.Exclude
    private List<Screen> screenings;
    private Layout layout;

    public Room(List<Screen> screenings, Layout layout) {
        this(UUID.randomUUID(), "Room", screenings, layout);
    }

    public Room(String name, List<Screen> screenings, Layout layout) {
        this(UUID.randomUUID(), name, screenings, layout);
    }

    public Room(UUID id, String name, List<Screen> screenings, Layout layout) {
        this.id = id;
        this.name = name;
        this.screenings = screenings;
        this.layout = layout;
    }
}
