package com.nilecom.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingTest {

    @Test
    void noDiscountBelowFive() {
        assertEquals(0.0, Pricing.discountFor(1));
        assertEquals(0.0, Pricing.discountFor(4));
    }

    @Test
    void tenPercentFromFive() {
        assertEquals(0.10, Pricing.discountFor(5));
        assertEquals(0.10, Pricing.discountFor(9));
    }

    @Test
    void fifteenPercentFromTen() {
        assertEquals(0.15, Pricing.discountFor(10));
        assertEquals(0.15, Pricing.discountFor(14));
    }

    @Test
    void twentyPercentFromFifteen() {
        assertEquals(0.20, Pricing.discountFor(15));
        assertEquals(0.20, Pricing.discountFor(100));
    }

    @Test
    void taxIsSixPercent() {
        assertEquals(6.0, Pricing.tax(100.0), 1e-9);
    }
}
