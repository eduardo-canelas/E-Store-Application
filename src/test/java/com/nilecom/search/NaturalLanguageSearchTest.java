package com.nilecom.search;

import com.nilecom.domain.Product;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NaturalLanguageSearchTest {

    private List<Product> catalogue() {
        return Arrays.asList(
                new Product("1", "3 ft mini USB cable M-F", true, 444, 4.50),
                new Product("2", "USB-C to USB-C cable 2m", true, 100, 12.00),
                new Product("11", "Apple MacBook Pro 14-inch M3 Chip", true, 30, 1999.00),
                new Product("3", "2025 CSX train calendar", true, 45, 12.95));
    }

    @Test
    void parsesUnderPriceConstraint() {
        NaturalLanguageSearch.Query q = NaturalLanguageSearch.parse("cheap usb cable under $10");
        assertEquals(10.0, q.maxPrice);
        assertNull(q.minPrice);
        assertEquals(NaturalLanguageSearch.Sort.PRICE_ASC, q.sort);
        assertTrue(q.terms.contains("usb"));
        assertTrue(q.terms.contains("cable"));
        assertFalse(q.terms.contains("cheap"));
        assertFalse(q.terms.contains("under"));
    }

    @Test
    void parsesOverPriceConstraint() {
        NaturalLanguageSearch.Query q = NaturalLanguageSearch.parse("laptop over 500");
        assertEquals(500.0, q.minPrice);
        assertTrue(q.terms.contains("laptop"));
    }

    @Test
    void filtersByPriceAndTerms() {
        List<Product> r = NaturalLanguageSearch.search("usb cable under $10", catalogue());
        assertEquals(1, r.size());
        assertEquals("1", r.get(0).id()); // only the $4.50 USB cable matches both terms + price
    }

    @Test
    void cheapSortsAscending() {
        List<Product> r = NaturalLanguageSearch.search("cheap cable", catalogue());
        assertEquals("1", r.get(0).id());
        assertTrue(r.get(0).price() <= r.get(r.size() - 1).price());
    }

    @Test
    void emptyQueryReturnsEverything() {
        List<Product> r = NaturalLanguageSearch.search("", catalogue());
        assertEquals(catalogue().size(), r.size());
    }

    @Test
    void bareDollarAmountTreatedAsMax() {
        NaturalLanguageSearch.Query q = NaturalLanguageSearch.parse("calendar $13");
        assertEquals(13.0, q.maxPrice);
        List<Product> r = NaturalLanguageSearch.apply(q, catalogue());
        assertEquals(1, r.size());
        assertEquals("3", r.get(0).id());
    }
}
