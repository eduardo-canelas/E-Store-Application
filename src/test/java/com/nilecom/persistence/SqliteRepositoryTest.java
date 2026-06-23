package com.nilecom.persistence;

import com.nilecom.domain.CartItem;
import com.nilecom.domain.Order;
import com.nilecom.domain.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqliteRepositoryTest {

    private Connection connection;
    private SqliteInventoryRepository inventory;
    private SqliteOrderRepository orderRepo;

    @BeforeEach
    void setUp() throws Exception {
        SqliteDatabase db = SqliteDatabase.inMemory();
        connection = db.open();
        db.initSchema(connection);
        db.seedIfEmpty(connection, Arrays.asList(
                new Product("1", "USB cable", true, 444, 4.50),
                new Product("11", "MacBook Pro", true, 30, 1999.00)));
        inventory = new SqliteInventoryRepository(connection);
        orderRepo = new SqliteOrderRepository(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void findByIdReturnsSeededProduct() {
        Optional<Product> p = inventory.findById("11");
        assertTrue(p.isPresent());
        assertEquals("MacBook Pro", p.get().description());
        assertEquals(1999.00, p.get().price(), 1e-9);
    }

    @Test
    void findByIdMissingIsEmpty() {
        assertFalse(inventory.findById("999").isPresent());
    }

    @Test
    void findAllReturnsAllSeeded() {
        assertEquals(2, inventory.findAll().size());
    }

    @Test
    void seedIsIdempotent() throws Exception {
        SqliteDatabase db = new SqliteDatabase(":memory:");
        // second seed call on already-populated table must not duplicate
        db.seedIfEmpty(connection, Arrays.asList(new Product("X", "Extra", true, 1, 1.0)));
        assertEquals(2, inventory.findAll().size());
    }

    @Test
    void saveAndReloadOrderRoundTrips() {
        Product mac = inventory.findById("11").orElseThrow();
        Product usb = inventory.findById("1").orElseThrow();
        Order order = new Order("TX1", "27/10/2025 14:45:54",
                Arrays.asList(new CartItem(mac, 2), new CartItem(usb, 16)),
                4070.20, 244.212, 4314.412);
        orderRepo.save(order);

        List<Order> all = orderRepo.findAll();
        assertEquals(1, all.size());
        Order reloaded = all.get(0);
        assertEquals("TX1", reloaded.transactionId());
        assertEquals(2, reloaded.lines().size());
        assertEquals(4314.412, reloaded.total(), 1e-6);
        assertEquals("11", reloaded.lines().get(0).id());
        assertEquals(2, reloaded.lines().get(0).quantity());
    }

    @Test
    void ordersReturnedMostRecentFirst() {
        Product usb = inventory.findById("1").orElseThrow();
        orderRepo.save(new Order("TXA", "t1", Arrays.asList(new CartItem(usb, 1)), 4.5, 0.27, 4.77));
        orderRepo.save(new Order("TXB", "t2", Arrays.asList(new CartItem(usb, 2)), 9.0, 0.54, 9.54));
        List<Order> all = orderRepo.findAll();
        assertEquals("TXB", all.get(0).transactionId());
        assertEquals("TXA", all.get(1).transactionId());
    }
}
