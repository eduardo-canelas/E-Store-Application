package com.nilecom.persistence;

import com.nilecom.domain.Product;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the legacy {@code inventory.csv} format:
 * {@code id, "description", inStock(bool), quantity, price}.
 * Used to seed the database on first run.
 */
public final class CsvInventoryImporter {

    private CsvInventoryImporter() {}

    /** Load the bundled seed catalogue from the classpath ({@code /inventory.csv}). */
    public static List<Product> fromClasspath() {
        try (InputStream in = CsvInventoryImporter.class.getResourceAsStream("/inventory.csv")) {
            if (in == null) return new ArrayList<>();
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bundled inventory.csv", e);
        }
    }

    public static List<Product> parse(Reader reader) throws IOException {
        List<Product> products = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] t = line.split(",");
                if (t.length < 5) continue;
                for (int i = 0; i < t.length; i++) {
                    // trim FIRST so a leading space before a quote doesn't defeat ^"
                    t[i] = t[i].trim().replaceAll("^\"|\"$", "").trim();
                }
                try {
                    products.add(new Product(
                            t[0],
                            t[1],
                            "true".equalsIgnoreCase(t[2]),
                            Integer.parseInt(t[3]),
                            Double.parseDouble(t[4])));
                } catch (NumberFormatException ignore) {
                    // skip malformed row
                }
            }
        }
        return products;
    }
}
