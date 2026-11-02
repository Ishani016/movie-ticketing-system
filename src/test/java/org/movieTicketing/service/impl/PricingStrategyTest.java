package org.movieTicketing.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingStrategyTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("10.00");

    @Test
    void normalRateChargesTheBasePriceUnchanged() {
        assertEquals(BASE_PRICE, new NormalRate().getPrice(BASE_PRICE));
    }

    @Test
    void premiumRateMultipliesTheBasePrice() {
        assertEquals(new BigDecimal("15.00"), new PremiumRate(new BigDecimal("1.5")).getPrice(BASE_PRICE));
    }

    @Test
    void luxeRateAddsAFlatSurcharge() {
        assertEquals(new BigDecimal("20.00"), new LuxeRate(new BigDecimal("10.00")).getPrice(BASE_PRICE));
    }

    @Test
    void theThreeStrategiesProduceDifferentPricesForTheSameBase() {
        BigDecimal normal = new NormalRate().getPrice(BASE_PRICE);
        BigDecimal premium = new PremiumRate().getPrice(BASE_PRICE);
        BigDecimal luxe = new LuxeRate().getPrice(BASE_PRICE);

        assertEquals(new BigDecimal("10.00"), normal);
        assertEquals(new BigDecimal("15.00"), premium);
        assertEquals(new BigDecimal("20.00"), luxe);
    }

    @Test
    void premiumRateRejectsNonPositiveMultiplier() {
        assertThrows(IllegalArgumentException.class, () -> new PremiumRate(BigDecimal.ZERO));
    }

    @Test
    void luxeRateRejectsNegativeSurcharge() {
        assertThrows(IllegalArgumentException.class, () -> new LuxeRate(new BigDecimal("-1")));
    }
}
