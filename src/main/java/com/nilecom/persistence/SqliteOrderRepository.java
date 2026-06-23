package com.nilecom.persistence;

import com.nilecom.domain.CartItem;
import com.nilecom.domain.Order;
import com.nilecom.domain.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persists and reconstructs completed orders in SQLite. */
public final class SqliteOrderRepository implements OrderRepository {

    private final Connection connection;

    public SqliteOrderRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(Order order) {
        try {
            connection.setAutoCommit(false);
            long orderPk;
            String insOrder = "INSERT INTO orders(tx_id, ts, subtotal, tax, total) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(insOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.transactionId());
                ps.setString(2, order.timestamp());
                ps.setDouble(3, order.subtotal());
                ps.setDouble(4, order.tax());
                ps.setDouble(5, order.total());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    orderPk = keys.getLong(1);
                }
            }
            String insLine = "INSERT INTO order_lines"
                    + "(order_pk, item_id, description, qty, unit_price, discount, line_total) "
                    + "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(insLine)) {
                for (CartItem it : order.lines()) {
                    ps.setLong(1, orderPk);
                    ps.setString(2, it.id());
                    ps.setString(3, it.description());
                    ps.setInt(4, it.quantity());
                    ps.setDouble(5, it.unitPrice());
                    ps.setDouble(6, it.discountRate());
                    ps.setDouble(7, it.lineTotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("save order failed", e);
        } finally {
            autoCommitQuietly();
        }
    }

    @Override
    public List<Order> findAll() {
        List<Order> out = new ArrayList<>();
        String sql = "SELECT pk, tx_id, ts, subtotal, tax, total FROM orders ORDER BY pk DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long pk = rs.getLong("pk");
                out.add(new Order(
                        rs.getString("tx_id"),
                        rs.getString("ts"),
                        loadLines(pk),
                        rs.getDouble("subtotal"),
                        rs.getDouble("tax"),
                        rs.getDouble("total")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll orders failed", e);
        }
        return out;
    }

    private List<CartItem> loadLines(long orderPk) throws SQLException {
        List<CartItem> lines = new ArrayList<>();
        String sql = "SELECT item_id, description, qty, unit_price, discount "
                + "FROM order_lines WHERE order_pk = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, orderPk);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product(
                            rs.getString("item_id"),
                            rs.getString("description"),
                            false, 0,
                            rs.getDouble("unit_price"));
                    lines.add(new CartItem(p, rs.getInt("qty"), rs.getDouble("discount")));
                }
            }
        }
        return lines;
    }

    private void rollbackQuietly() {
        try { connection.rollback(); } catch (SQLException ignore) {}
    }

    private void autoCommitQuietly() {
        try { connection.setAutoCommit(true); } catch (SQLException ignore) {}
    }
}
