package org.movieTicketing.models;

import org.junit.jupiter.api.Test;
import org.movieTicketing.service.impl.LuxeRate;
import org.movieTicketing.service.impl.NormalRate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SeatTest {

    @Test
    void seatsWithTheSameNumberAreEqualRegardlessOfStrategy() {
        Seat a = new Seat("1-1", new NormalRate());
        Seat b = new Seat("1-1", new LuxeRate());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void seatsWithDifferentNumbersAreNeverEqual() {
        Seat a = new Seat("1-1", new NormalRate());
        Seat b = new Seat("1-2", new NormalRate());

        assertNotEquals(a, b);
    }
}
