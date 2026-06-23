package com.nilecom.domain;

/** A product plus a chosen quantity and the discount that quantity earns. Immutable. */
public final class CartItem {
    private final Product product;
    private final int quantity;
    private final double discountRate;

    public CartItem(Product product, int quantity) {
        this(product, quantity, Pricing.discountFor(quantity));
    }

    public CartItem(Product product, int quantity, double discountRate) {
        this.product = product;
        this.quantity = quantity;
        this.discountRate = discountRate;
    }

    public Product product() { return product; }
    public int quantity() { return quantity; }
    public double discountRate() { return discountRate; }

    public String id() { return product.id(); }
    public String description() { return product.description(); }
    public double unitPrice() { return product.price(); }
    public int available() { return product.quantityAvailable(); }

    public double grossTotal() { return product.price() * quantity; }
    public double lineTotal() { return grossTotal() * (1.0 - discountRate); }
    public double saved() { return grossTotal() - lineTotal(); }
}
