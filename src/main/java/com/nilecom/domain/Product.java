package com.nilecom.domain;

/** Immutable catalogue product. */
public final class Product {
    private final String id;
    private final String description;
    private final boolean inStock;
    private final int quantityAvailable;
    private final double price;

    public Product(String id, String description, boolean inStock,
                   int quantityAvailable, double price) {
        this.id = id;
        this.description = description;
        this.inStock = inStock;
        this.quantityAvailable = quantityAvailable;
        this.price = price;
    }

    public String id() { return id; }
    public String description() { return description; }
    public boolean inStock() { return inStock; }
    public int quantityAvailable() { return quantityAvailable; }
    public double price() { return price; }

    /** True only when flagged in stock AND at least one unit remains. */
    public boolean isAvailable() {
        return inStock && quantityAvailable > 0;
    }
}
