package com.nilecom.persistence;

import com.nilecom.domain.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads the product catalogue from SQLite. */
public final class SqliteInventoryRepository implements InventoryRepository {

    private final Connection connection;

    public SqliteInventoryRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Product> findById(String id) {
        String sql = "SELECT id, description, in_stock, quantity, price FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT id, description, in_stock, quantity, price FROM products "
                + "ORDER BY CAST(id AS INTEGER)";
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
        return out;
    }

    private Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getString("id"),
                rs.getString("description"),
                rs.getInt("in_stock") == 1,
                rs.getInt("quantity"),
                rs.getDouble("price"));
    }
}
