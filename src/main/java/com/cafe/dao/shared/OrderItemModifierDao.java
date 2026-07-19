package com.cafe.dao.shared;

import com.cafe.model.OrderItemModifier;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderItemModifierDao {

    public void insert(Connection conn, int orderItemId, int modifierOptionId, BigDecimal priceDelta) throws SQLException {
        final String sql = "INSERT INTO sales.OrderItemModifier(OrderItemId, ModifierOptionId, PriceDelta) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            ps.setInt(2, modifierOptionId);
            ps.setBigDecimal(3, priceDelta == null ? BigDecimal.ZERO : priceDelta);
            ps.executeUpdate();
        }
    }

    public List<OrderItemModifier> findByItem(Connection conn, int orderItemId) throws SQLException {
        final String sql =
            "SELECT oim.OrderItemModifierId, oim.OrderItemId, oim.ModifierOptionId, oim.PriceDelta, mo.Name AS OptionName " +
            "FROM sales.OrderItemModifier oim " +
            "JOIN catalog.ModifierOption mo ON mo.ModifierOptionId=oim.ModifierOptionId " +
            "WHERE oim.OrderItemId=? ORDER BY oim.OrderItemModifierId";
        List<OrderItemModifier> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItemModifier m = new OrderItemModifier();
                    m.setOrderItemModifierId(rs.getInt("OrderItemModifierId"));
                    m.setOrderItemId(rs.getInt("OrderItemId"));
                    m.setModifierOptionId(rs.getInt("ModifierOptionId"));
                    m.setPriceDelta(rs.getBigDecimal("PriceDelta"));
                    m.setOptionName(rs.getString("OptionName"));
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** Các ModifierOptionId khách đã chọn cho dòng đơn — đầu vào auto-deduct. */
    public List<Integer> findOptionIds(Connection conn, int orderItemId) throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ModifierOptionId FROM sales.OrderItemModifier WHERE OrderItemId=?")) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getInt(1)); }
        }
        return out;
    }
}
