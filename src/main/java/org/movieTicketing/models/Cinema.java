package org.movieTicketing.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cinema {
    @EqualsAndHashCode.Include
    private final UUID id;
    private String name;
    private Location location;
    private List<Room> rooms;

    public Cinema(String name, Location location, List<Room> rooms) {
        this(UUID.randomUUID(), name, location, rooms);
    }

    public Cinema(UUID id, String name, Location location, List<Room> rooms) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.rooms = rooms;
    }
}
