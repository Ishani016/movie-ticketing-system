package org.movieTicketing.service.impl;

import org.movieTicketing.service.SeatPricingStrategy;

import java.math.BigDecimal;

/**
 * Standard seats are charged at the screening's base price, unchanged.
 */
public class NormalRate implements SeatPricingStrategy {
    @Override
    public BigDecimal getPrice(BigDecimal basePrice) {
        return basePrice;
    }
}
