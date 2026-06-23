package com.nilecom.domain;

/** Pricing rules: quantity discount tiers and sales tax. Pure, side-effect free. */
public final class Pricing {
    public static final double TAX_RATE = 0.06;

    private Pricing() {}

    /**
     * Bulk discount by quantity:
     * 15+ -> 20%, 10-14 -> 15%, 5-9 -> 10%, otherwise none.
     */
    public static double discountFor(int quantity) {
        if (quantity >= 15) return 0.20;
        if (quantity >= 10) return 0.15;
        if (quantity >= 5) return 0.10;
        return 0.0;
    }

    public static double tax(double subtotal) {
        return subtotal * TAX_RATE;
    }
}
