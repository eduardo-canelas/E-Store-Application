package com.nilecom.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mutable shopping cart aggregate: capped line items, derived money totals. */
public final class Cart {
    public static final int MAX_ITEMS = 5;

    private final List<CartItem> items = new ArrayList<>();

    public boolean isFull() { return items.size() >= MAX_ITEMS; }
    public boolean isEmpty() { return items.isEmpty(); }
    public int size() { return items.size(); }

    /** @return false when the cart is already at {@link #MAX_ITEMS}. */
    public boolean add(CartItem item) {
        if (isFull()) return false;
        items.add(item);
        return true;
    }

    public void removeAt(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    public void clear() { items.clear(); }

    public List<CartItem> items() { return Collections.unmodifiableList(items); }

    public double subtotal() {
        double sum = 0;
        for (CartItem it : items) sum += it.lineTotal();
        return sum;
    }

    public double tax() { return Pricing.tax(subtotal()); }

    public double total() { return subtotal() + tax(); }

    public double saved() {
        double sum = 0;
        for (CartItem it : items) sum += it.saved();
        return sum;
    }
}
