package org.movieTicketing.service.impl;

import org.movieTicketing.service.SeatPricingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Luxe seats add a flat surcharge on top of the screening's base price.
 */
public class LuxeRate implements SeatPricingStrategy {
    private static final BigDecimal DEFAULT_SURCHARGE = new BigDecimal("10.00");

    private final BigDecimal surcharge;

    public LuxeRate() {
        this(DEFAULT_SURCHARGE);
    }

    public LuxeRate(BigDecimal surcharge) {
        if (surcharge == null || surcharge.signum() < 0) {
            throw new IllegalArgumentException("surcharge must not be negative");
        }
        this.surcharge = surcharge;
    }

    @Override
    public BigDecimal getPrice(BigDecimal basePrice) {
        return basePrice.add(surcharge).setScale(2, RoundingMode.HALF_UP);
    }
}
