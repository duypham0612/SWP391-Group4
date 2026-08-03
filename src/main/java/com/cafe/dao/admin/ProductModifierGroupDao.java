package com.cafe.dao.admin;

import com.cafe.model.ProductModifierGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Adapter đọc nhóm modifier của sản phẩm từ quan hệ 1-N catalog.ModifierGroup.ProductId. */
public class ProductModifierGroupDao {
    public List<ProductModifierGroup> findByProduct(Connection conn, int productId) throws SQLException {
        String sql = "SELECT ProductId,ModifierGroupId,Name AS GroupName,SortOrder "
                + "FROM catalog.ModifierGroup WHERE ProductId=? ORDER BY SortOrder,ModifierGroupId";
        List<ProductModifierGroup> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductModifierGroup row = new ProductModifierGroup();
                    row.setProductId(rs.getInt("ProductId"));
                    row.setModifierGroupId(rs.getInt("ModifierGroupId"));
                    row.setGroupName(rs.getString("GroupName"));
                    row.setSortOrder(rs.getInt("SortOrder"));
                    out.add(row);
                }
            }
        }
        return out;
    }
}
