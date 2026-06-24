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
        String sql = "SELECT ProductID, ProductName, BasePrice, CategoryID FROM Products WHERE IsAvailable = 1";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getDouble("BasePrice"),
                        rs.getInt("CategoryID")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}