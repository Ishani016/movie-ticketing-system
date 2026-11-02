package org.movieTicketing.models;

import org.junit.jupiter.api.Test;
import org.movieTicketing.service.impl.LuxeRate;
import org.movieTicketing.service.impl.NormalRate;
import org.movieTicketing.service.impl.PremiumRate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutTest {

    @Test
    void everySeatGetsANonNullDefaultStrategy() {
        Layout layout = new Layout(2, 3, new NormalRate());

        assertEquals(6, layout.getAllSeats().size());
        layout.getAllSeats().forEach(seat -> assertInstanceOf(NormalRate.class, seat.getSeatPricingStrategy()));
    }

    @Test
    void rowOverridesWinOverTheDefaultStrategy() {
        Layout layout = new Layout(3, 2, new NormalRate(), Map.of(0, new PremiumRate(), 2, new LuxeRate()));

        assertInstanceOf(PremiumRate.class, layout.getSeatAt(0, 0).orElseThrow().getSeatPricingStrategy());
        assertInstanceOf(NormalRate.class, layout.getSeatAt(1, 0).orElseThrow().getSeatPricingStrategy());
        assertInstanceOf(LuxeRate.class, layout.getSeatAt(2, 0).orElseThrow().getSeatPricingStrategy());
    }

    @Test
    void getSeatAtReturnsEmptyForOutOfRangePosition() {
        Layout layout = new Layout(1, 1, new NormalRate());

        assertTrue(layout.getSeatAt(5, 5).isEmpty());
    }

    @Test
    void nullDefaultStrategyIsRejected() {
        assertThrows(NullPointerException.class, () -> new Layout(1, 1, null));
    }

    @Test
    void bookingPriceUsingLayoutSeatNeverThrowsNpe() {
        Layout layout = new Layout(1, 1, new NormalRate());
        Seat seat = layout.getAllSeats().get(0);

        BigDecimal price = seat.getSeatPricingStrategy().getPrice(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("10.00"), price);
    }
}
