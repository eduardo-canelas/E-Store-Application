package com.nilecom.persistence;

import com.nilecom.domain.Product;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvInventoryImporterTest {

    @Test
    void parsesQuotedCsvRows() throws IOException {
        String csv = "1, \"3 ft mini USB cable M-F\", true, 444, 4.50\n"
                + "2, \"EZ-ink cartridge set for Epson 4630\", false, 0, 75.00\n";
        List<Product> products = CsvInventoryImporter.parse(new StringReader(csv));
        assertEquals(2, products.size());
        Product first = products.get(0);
        assertEquals("1", first.id());
        assertEquals("3 ft mini USB cable M-F", first.description());
        assertTrue(first.inStock());
        assertEquals(444, first.quantityAvailable());
        assertEquals(4.50, first.price(), 1e-9);
        assertFalse(products.get(1).isAvailable());
    }

    @Test
    void skipsBlankAndMalformedRows() throws IOException {
        String csv = "\n1, \"Good\", true, 5, 1.00\nbroken,row\n";
        List<Product> products = CsvInventoryImporter.parse(new StringReader(csv));
        assertEquals(1, products.size());
    }

    @Test
    void loadsBundledSeedFromClasspath() {
        List<Product> products = CsvInventoryImporter.fromClasspath();
        assertFalse(products.isEmpty(), "bundled /inventory.csv should be present and parseable");
    }
}
