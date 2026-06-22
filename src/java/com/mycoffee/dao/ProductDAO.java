package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    public List<Product> getAllAvailableProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, p.CategoryID, "
                   + "c.CategoryName, p.ImageURL, p.Description "
                   + "FROM Products p "
                   + "LEFT JOIN Categories c ON p.CategoryID = c.CategoryID "
                   + "WHERE p.IsAvailable = 1 "
                   + "ORDER BY p.CategoryID, p.ProductName";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Product getAvailableProductById(int productId) {
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, p.CategoryID, "
                   + "c.CategoryName, p.ImageURL, p.Description "
                   + "FROM Products p "
                   + "LEFT JOIN Categories c ON p.CategoryID = c.CategoryID "
                   + "WHERE p.ProductID = ? AND p.IsAvailable = 1";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Product mapProduct(ResultSet rs) throws Exception {
        return new Product(
                rs.getInt("ProductID"),
                rs.getString("ProductName"),
                rs.getDouble("BasePrice"),
                rs.getInt("CategoryID"),
                rs.getString("CategoryName"),
                rs.getString("ImageURL"),
                rs.getString("Description")
        );
    }
}
