package org.movieTicketing.service.impl;

import org.movieTicketing.service.SeatPricingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Premium seats cost a multiple of the screening's base price.
 */
public class PremiumRate implements SeatPricingStrategy {
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("1.5");

    private final BigDecimal multiplier;

    public PremiumRate() {
        this(DEFAULT_MULTIPLIER);
    }

    public PremiumRate(BigDecimal multiplier) {
        if (multiplier == null || multiplier.signum() <= 0) {
            throw new IllegalArgumentException("multiplier must be positive");
        }
        this.multiplier = multiplier;
    }

    @Override
    public BigDecimal getPrice(BigDecimal basePrice) {
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
