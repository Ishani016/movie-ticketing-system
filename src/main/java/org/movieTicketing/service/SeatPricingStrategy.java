package org.movieTicketing.service;

import java.math.BigDecimal;

public interface SeatPricingStrategy {
    /**
     * @param basePrice the screening's base ticket price
     * @return the final price for a seat using this strategy
     */
    BigDecimal getPrice(BigDecimal basePrice);
}
