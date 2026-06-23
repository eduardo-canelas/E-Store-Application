package com.nilecom.persistence;

import com.nilecom.domain.Product;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Owns the SQLite connection and schema lifecycle. Embedded, file-based (or
 * in-memory for tests) — no server, single jar dependency (xerial sqlite-jdbc).
 */
public final class SqliteDatabase {

    private final String url;

    public SqliteDatabase(String path) {
        this.url = "jdbc:sqlite:" + path;
    }

    /** In-memory database shared for the life of one connection — handy for tests. */
    public static SqliteDatabase inMemory() {
        return new SqliteDatabase(":memory:");
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void initSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS products (" +
                "  id TEXT PRIMARY KEY," +
                "  description TEXT NOT NULL," +
                "  in_stock INTEGER NOT NULL," +
                "  quantity INTEGER NOT NULL," +
                "  price REAL NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "  pk INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  tx_id TEXT NOT NULL," +
                "  ts TEXT NOT NULL," +
                "  subtotal REAL NOT NULL," +
                "  tax REAL NOT NULL," +
                "  total REAL NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS order_lines (" +
                "  order_pk INTEGER NOT NULL," +
                "  item_id TEXT NOT NULL," +
                "  description TEXT NOT NULL," +
                "  qty INTEGER NOT NULL," +
                "  unit_price REAL NOT NULL," +
                "  discount REAL NOT NULL," +
                "  line_total REAL NOT NULL," +
                "  FOREIGN KEY(order_pk) REFERENCES orders(pk))");
        }
    }

    /** Insert products only when the table is empty (idempotent first-run seed). */
    public void seedIfEmpty(Connection c, List<Product> products) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM products")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO products(id, description, in_stock, quantity, price) VALUES (?,?,?,?,?)";
        c.setAutoCommit(false);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.id());
                ps.setString(2, p.description());
                ps.setInt(3, p.inStock() ? 1 : 0);
                ps.setInt(4, p.quantityAvailable());
                ps.setDouble(5, p.price());
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
}
