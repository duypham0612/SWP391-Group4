package com.cafe.dao.shared;

import com.cafe.model.OrderItemModifier;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class OrderItemModifierDao {

    private static final String SELECT =
        "SELECT oim.OrderItemModifierId, oim.OrderItemId, oim.ModifierOptionId, oim.PriceDelta, mo.Name AS OptionName " +
        "FROM sales.OrderItemModifier oim " +
        "JOIN catalog.ModifierOption mo ON mo.ModifierOptionId=oim.ModifierOptionId ";

    /** SQL Server chỉ nhận 2100 tham số cho một câu lệnh — chia lô để danh sách dài không vỡ. */
    private static final int ID_CHUNK = 1000;

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
        List<OrderItemModifier> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE oim.OrderItemId=? ORDER BY oim.OrderItemModifierId")) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Modifier của NHIỀU dòng đơn trong một query — thay cho việc gọi {@link #findByItem} từng món
     * (N+1) khi nạp cả board quầy pha chế hoặc cả đơn.
     *
     * <p>Trả map theo OrderItemId; món không có modifier sẽ KHÔNG có khoá trong map, caller tự
     * quyết định giá trị mặc định.
     */
    public Map<Integer, List<OrderItemModifier>> findByItems(Connection conn, Collection<Integer> orderItemIds)
            throws SQLException {
        Map<Integer, List<OrderItemModifier>> out = new HashMap<>();
        if (orderItemIds == null || orderItemIds.isEmpty()) return out;
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(orderItemIds));   // bỏ trùng
        for (int from = 0; from < ids.size(); from += ID_CHUNK) {
            List<Integer> chunk = ids.subList(from, Math.min(from + ID_CHUNK, ids.size()));
            StringBuilder marks = new StringBuilder();
            for (int i = 0; i < chunk.size(); i++) marks.append(i == 0 ? "?" : ",?");
            // Sắp theo OrderItemId trước để thứ tự modifier trong từng món khớp findByItem.
            try (PreparedStatement ps = conn.prepareStatement(SELECT
                    + "WHERE oim.OrderItemId IN (" + marks + ") "
                    + "ORDER BY oim.OrderItemId, oim.OrderItemModifierId")) {
                for (int i = 0; i < chunk.size(); i++) ps.setInt(i + 1, chunk.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        OrderItemModifier m = map(rs);
                        out.computeIfAbsent(m.getOrderItemId(), k -> new ArrayList<>()).add(m);
                    }
                }
            }
        }
        return out;
    }

    private static OrderItemModifier map(ResultSet rs) throws SQLException {
        OrderItemModifier m = new OrderItemModifier();
        m.setOrderItemModifierId(rs.getInt("OrderItemModifierId"));
        m.setOrderItemId(rs.getInt("OrderItemId"));
        m.setModifierOptionId(rs.getInt("ModifierOptionId"));
        m.setPriceDelta(rs.getBigDecimal("PriceDelta"));
        m.setOptionName(rs.getString("OptionName"));
        return m;
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
