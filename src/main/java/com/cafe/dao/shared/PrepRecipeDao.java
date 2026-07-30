package com.cafe.dao.shared;

import com.cafe.model.PrepRecipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrepRecipeDao {

    private static final String SELECT =
        "SELECT pr.PrepRecipeId, pr.PreppedIngredientId, pr.RawIngredientId, pr.Quantity, pr.YieldQty, " +
        "       i.Name AS RawName, i.Unit AS RawUnit " +
        "FROM catalog.PrepRecipe pr JOIN catalog.Ingredient i ON pr.RawIngredientId = i.IngredientId ";

    public List<PrepRecipe> findByPrepped(Connection conn, int preppedIngredientId) throws SQLException {
        List<PrepRecipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE pr.PreppedIngredientId = ? ORDER BY i.Name")) {
            ps.setInt(1, preppedIngredientId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Công thức của NHIỀU nguyên liệu pha sẵn trong một lượt truy vấn (tránh N+1 khi dựng màn Prep).
     * Mọi id truyền vào đều có mặt trong map trả về, nguyên liệu chưa khai báo công thức thì nhận list rỗng.
     * Vẫn ORDER BY tên nguyên liệu thô trong từng nhóm để thứ tự khoá dòng đồng nhất với findByPrepped.
     */
    public Map<Integer, List<PrepRecipe>> findByPreppedIds(Connection conn, List<Integer> preppedIngredientIds)
            throws SQLException {
        Map<Integer, List<PrepRecipe>> out = new LinkedHashMap<>();
        if (preppedIngredientIds == null || preppedIngredientIds.isEmpty()) return out;
        for (Integer id : preppedIngredientIds) if (id != null) out.putIfAbsent(id, new ArrayList<>());
        if (out.isEmpty()) return out;

        List<Integer> ids = new ArrayList<>(out.keySet());
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
        String sql = SELECT + "WHERE pr.PreppedIngredientId IN (" + placeholders + ") "
                + "ORDER BY pr.PreppedIngredientId, i.Name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrepRecipe pr = map(rs);
                    out.get(pr.getPreppedIngredientId()).add(pr);
                }
            }
        }
        return out;
    }

    private PrepRecipe map(ResultSet rs) throws SQLException {
        PrepRecipe pr = new PrepRecipe();
        pr.setPrepRecipeId(rs.getInt("PrepRecipeId"));
        pr.setPreppedIngredientId(rs.getInt("PreppedIngredientId"));
        pr.setRawIngredientId(rs.getInt("RawIngredientId"));
        pr.setQuantity(rs.getBigDecimal("Quantity"));
        pr.setYieldQty(rs.getBigDecimal("YieldQty"));
        pr.setRawIngredientName(rs.getString("RawName"));
        pr.setRawIngredientUnit(rs.getString("RawUnit"));
        return pr;
    }

    public void insert(Connection conn, PrepRecipe pr) throws SQLException {
        final String sql = "INSERT INTO catalog.PrepRecipe(PreppedIngredientId, RawIngredientId, Quantity, YieldQty) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pr.getPreppedIngredientId());
            ps.setInt(2, pr.getRawIngredientId());
            ps.setBigDecimal(3, pr.getQuantity());
            ps.setBigDecimal(4, pr.getYieldQty());
            ps.executeUpdate();
        }
    }

    public int updateQuantity(Connection conn, int prepRecipeId, int preppedIngredientId,
                              java.math.BigDecimal quantity) throws SQLException {
        final String sql =
                "UPDATE catalog.PrepRecipe SET Quantity = ? " +
                "WHERE PrepRecipeId = ? AND PreppedIngredientId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, prepRecipeId);
            ps.setInt(3, preppedIngredientId);
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, int prepRecipeId, int preppedIngredientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.PrepRecipe WHERE PrepRecipeId = ? AND PreppedIngredientId = ?")) {
            ps.setInt(1, prepRecipeId);
            ps.setInt(2, preppedIngredientId);
            return ps.executeUpdate();
        }
    }
}
