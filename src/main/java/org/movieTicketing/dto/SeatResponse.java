package org.movieTicketing.dto;

import org.movieTicketing.models.Seat;
import org.movieTicketing.service.impl.LuxeRate;
import org.movieTicketing.service.impl.NormalRate;
import org.movieTicketing.service.impl.PremiumRate;

import java.math.BigDecimal;

public record SeatResponse(
        String seatNumber,
        String pricingTier,
        BigDecimal price
) {
    public static SeatResponse from(Seat seat, BigDecimal basePrice) {
        return new SeatResponse(
                seat.getSeatNumber(),
                pricingTier(seat),
                seat.getSeatPricingStrategy().getPrice(basePrice)
        );
    }

    private static String pricingTier(Seat seat) {
        var strategy = seat.getSeatPricingStrategy();
        if (strategy instanceof PremiumRate) {
            return "PREMIUM";
        }
        if (strategy instanceof LuxeRate) {
            return "LUXE";
        }
        if (strategy instanceof NormalRate) {
            return "NORMAL";
        }
        return strategy.getClass().getSimpleName();
    }
}
