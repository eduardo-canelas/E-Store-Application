package com.nilecom.persistence;

import com.nilecom.domain.CartItem;
import com.nilecom.domain.Order;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

/**
 * Append-only transaction log in the original assignment CSV format. Retained
 * alongside the SQLite order store for backward compatibility / audit trail.
 */
public final class CsvTransactionLog {
    private static final DecimalFormat MONEY = new DecimalFormat("$#,##0.00");
    private final String path;

    public CsvTransactionLog(String path) {
        this.path = path;
    }

    public void append(Order order) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(path, true))) {
            for (CartItem it : order.lines()) {
                out.println(order.transactionId() + "," + order.timestamp()
                        + ",Item: " + it.id()
                        + ", Description: " + it.description()
                        + ", Price: " + MONEY.format(it.unitPrice())
                        + ", Qty: " + it.quantity()
                        + ", Subtotal: " + MONEY.format(it.lineTotal()));
            }
        }
    }
}
