package com.nilecom.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A completed purchase: a transaction id, timestamp, frozen lines and money totals. */
public final class Order {
    private final String transactionId;
    private final String timestamp;
    private final List<CartItem> lines;
    private final double subtotal;
    private final double tax;
    private final double total;

    public Order(String transactionId, String timestamp, List<CartItem> lines,
                 double subtotal, double tax, double total) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
    }

    public static Order fromCart(String transactionId, String timestamp, Cart cart) {
        return new Order(transactionId, timestamp, cart.items(),
                cart.subtotal(), cart.tax(), cart.total());
    }

    public String transactionId() { return transactionId; }
    public String timestamp() { return timestamp; }
    public List<CartItem> lines() { return lines; }
    public double subtotal() { return subtotal; }
    public double tax() { return tax; }
    public double total() { return total; }

    public double saved() {
        double sum = 0;
        for (CartItem it : lines) sum += it.saved();
        return sum;
    }
}
