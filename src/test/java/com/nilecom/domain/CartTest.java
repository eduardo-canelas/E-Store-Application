package com.nilecom.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private Product product(String id, double price, int available) {
        return new Product(id, "Item " + id, true, available, price);
    }

    @Test
    void capsAtMaxItems() {
        Cart cart = new Cart();
        for (int i = 0; i < Cart.MAX_ITEMS; i++) {
            assertTrue(cart.add(new CartItem(product("" + i, 1.0, 99), 1)));
        }
        assertTrue(cart.isFull());
        assertFalse(cart.add(new CartItem(product("x", 1.0, 99), 1)));
        assertEquals(Cart.MAX_ITEMS, cart.size());
    }

    @Test
    void removeByIndex() {
        Cart cart = new Cart();
        cart.add(new CartItem(product("1", 10, 99), 1));
        cart.add(new CartItem(product("2", 20, 99), 1));
        cart.removeAt(0);
        assertEquals(1, cart.size());
        assertEquals("2", cart.items().get(0).id());
    }

    @Test
    void removeOutOfRangeIsSafe() {
        Cart cart = new Cart();
        cart.add(new CartItem(product("1", 10, 99), 1));
        cart.removeAt(5);
        cart.removeAt(-1);
        assertEquals(1, cart.size());
    }

    @Test
    void subtotalTaxTotalAndSaved() {
        Cart cart = new Cart();
        // 2 x 1999.00 -> no discount (tier starts at 5) = 3998.00
        // 16 x 6.50  -> 20% = 83.20 ; 1 x 12.95 = 12.95
        cart.add(new CartItem(product("11", 1999.00, 30), 2));
        cart.add(new CartItem(product("4", 6.50, 6690), 16));
        cart.add(new CartItem(product("3", 12.95, 45), 1));

        assertEquals(4094.15, cart.subtotal(), 1e-6);
        assertEquals(245.649, cart.tax(), 1e-6);
        assertEquals(4339.799, cart.total(), 1e-6);
        // saved: only the 16-unit line earns 20%: 104.00 - 83.20 = 20.80
        assertEquals(20.80, cart.saved(), 1e-6);
    }

    @Test
    void bulkLineEarnsDiscountFromQuantity() {
        Cart cart = new Cart();
        cart.add(new CartItem(product("9", 10.0, 100), 5)); // 5 -> 10%
        assertEquals(45.0, cart.subtotal(), 1e-9);          // 50 * 0.90
        assertEquals(5.0, cart.saved(), 1e-9);
    }

    @Test
    void itemsViewIsUnmodifiable() {
        Cart cart = new Cart();
        cart.add(new CartItem(product("1", 10, 99), 1));
        assertThrows(UnsupportedOperationException.class,
                () -> cart.items().add(new CartItem(product("2", 1, 1), 1)));
    }
}
