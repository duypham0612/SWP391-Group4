package com.cafe.dao.shared;

import com.cafe.model.StockReceiptDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockReceiptDetailDao {

    public List<StockReceiptDetail> findByReceipt(Connection conn, int receiptId) throws SQLException {
        final String sql =
            "SELECT d.StockReceiptDetailId, d.StockReceiptId, d.IngredientId, d.Quantity, d.UnitCost, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit " +
            "FROM inventory.StockReceiptDetail d JOIN catalog.Ingredient i ON d.IngredientId = i.IngredientId " +
            "WHERE d.StockReceiptId = ? ORDER BY d.StockReceiptDetailId";
        List<StockReceiptDetail> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockReceiptDetail d = new StockReceiptDetail();
                    d.setStockReceiptDetailId(rs.getInt("StockReceiptDetailId"));
                    d.setStockReceiptId(rs.getInt("StockReceiptId"));
                    d.setIngredientId(rs.getInt("IngredientId"));
                    d.setQuantity(rs.getBigDecimal("Quantity"));
                    d.setUnitCost(rs.getBigDecimal("UnitCost"));
                    d.setIngredientName(rs.getString("IngredientName"));
                    d.setIngredientUnit(rs.getString("IngredientUnit"));
                    out.add(d);
                }
            }
        }
        return out;
    }

    public void insert(Connection conn, StockReceiptDetail d) throws SQLException {
        final String sql = "INSERT INTO inventory.StockReceiptDetail(StockReceiptId, IngredientId, Quantity, UnitCost) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getStockReceiptId());
            ps.setInt(2, d.getIngredientId());
            ps.setBigDecimal(3, d.getQuantity());
            ps.setBigDecimal(4, d.getUnitCost());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int detailId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM inventory.StockReceiptDetail WHERE StockReceiptDetailId=?")) {
            ps.setInt(1, detailId);
            ps.executeUpdate();
        }
    }
}
