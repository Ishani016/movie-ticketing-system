package org.movieTicketing.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.movieTicketing.service.SeatPricingStrategy;

@AllArgsConstructor
@Data
public class Seat {
    private String seatNumber;

    // Excluded: a seat's identity is its number within a room, not its current
    // pricing strategy. Including it broke seat-matching in getAvailableSeats()/
    // isSeatBooked() whenever two Seat instances carried different strategy objects.
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private SeatPricingStrategy seatPricingStrategy;
}

