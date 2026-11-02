package org.movieTicketing.models;

import org.junit.jupiter.api.Test;
import org.movieTicketing.service.impl.NormalRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Screen.room and Room.screenings form a two-way reference. Before the fix,
 * @Data-generated equals/hashCode/toString recursed through that cycle
 * (Screen -> Room -> screenings -> Screen -> ...) and blew the stack the
 * moment a Room's screening list was actually populated, which is exactly
 * what these tests do.
 */
class ScreenRoomCycleTest {

    @Test
    void hashCodeDoesNotRecurseThroughTheCycle() {
        List<Screen> screenings = new ArrayList<>();
        Room room = new Room(screenings, new Layout(1, 1, new NormalRate()));
        Movie movie = new Movie("Dune", Genre.SCI_FI, 155L);
        Screen screen = new Screen(UUID.randomUUID(), movie, LocalDateTime.now(),
                LocalDateTime.now().plusHours(2), room, new BigDecimal("10.00"));
        screenings.add(screen); // Room now references back to the Screen that references it.

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            screen.hashCode();
            room.hashCode();
            screen.toString();
            room.toString();
            screen.equals(screen);
        });
    }

    @Test
    void screenIdentityIsBasedOnIdOnly() {
        UUID id = UUID.randomUUID();
        Movie movie = new Movie("Dune", Genre.SCI_FI, 155L);
        LocalDateTime start = LocalDateTime.now();
        Room roomA = new Room(new ArrayList<>(), new Layout(1, 1, new NormalRate()));
        Room roomB = new Room(new ArrayList<>(), new Layout(2, 2, new NormalRate()));

        Screen screenA = new Screen(id, movie, start, start.plusHours(2), roomA, new BigDecimal("10.00"));
        Screen screenB = new Screen(id, movie, start, start.plusHours(2), roomB, new BigDecimal("99.00"));

        assertEquals(screenA, screenB);
    }
}
