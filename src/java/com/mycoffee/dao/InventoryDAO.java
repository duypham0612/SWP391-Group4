/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Inventory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ACER
 */
public class InventoryDAO {

    // HÀM LẤY DANH SÁCH TỒN KHO THEO TỪNG CHI NHÁNH
    public List<Inventory> getInventoryByBranch(int branchId) {
        List<Inventory> list = new ArrayList<>();
        // Câu lệnh SQL lấy dữ liệu tồn kho kết hợp với tên nguyên liệu từ bảng phụ
        String sql = "SELECT i.BranchID, i.IngredientID, i.Quantity, i.MinRequired, i.LastUpdated, "
                + "       ing.IngredientName, ing.Unit "
                + "FROM Inventory i "
                + "JOIN Ingredients ing ON i.IngredientID = ing.IngredientID "
                + "WHERE i.BranchID = ?";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Inventory item = new Inventory();
                    item.setBranchId(rs.getInt("BranchID"));
                    item.setIngredientId(rs.getInt("IngredientID"));
                    item.setQuantity(rs.getDouble("Quantity"));
                    item.setMinRequired(rs.getDouble("MinRequired"));
                    item.setLastUpdated(rs.getTimestamp("LastUpdated"));
                    item.setIngredientName(rs.getString("IngredientName"));
                    item.setUnit(rs.getString("Unit"));
                    list.add(item);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham getInventoryByBranch trong InventoryDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}
