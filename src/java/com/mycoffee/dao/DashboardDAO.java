package com.mycoffee.dao;

import com.mycoffee.context.DBContext; // Thay thế bằng tên file kết nối DB thật của bạn nếu khác
import com.mycoffee.model.DashboardStats;
import com.mycoffee.model.DashboardStats.OrderSummary;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO extends DBContext { // Hoặc khởi tạo kết nối thông thường tùy project của bạn

    public DashboardStats getDashboardData() {
        DashboardStats stats = new DashboardStats();
        List<OrderSummary> recentOrders = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // Giả sử DBContext của bạn cung cấp hàm getConnection() công khai
            conn = getConnection(); 

            // 1. Lấy tổng doanh thu hôm nay
            String sqlRevenue = "SELECT SUM(FinalAmount) FROM Orders WHERE CAST(OrderDate AS DATE) = CAST(GETDATE() AS DATE) AND OrderStatus = 'Completed'";
            ps = conn.prepareStatement(sqlRevenue);
            rs = ps.executeQuery();
            if (rs.next()) stats.setTotalRevenue(rs.getDouble(1));
            rs.close(); ps.close();

            // 2. Lấy tổng số đơn hàng hôm nay
            String sqlOrders = "SELECT COUNT(*) FROM Orders WHERE CAST(OrderDate AS DATE) = CAST(GETDATE() AS DATE)";
            ps = conn.prepareStatement(sqlOrders);
            rs = ps.executeQuery();
            if (rs.next()) stats.setTotalOrders(rs.getInt(1));
            rs.close(); ps.close();

            // 3. Lấy số lượng nguyên liệu sắp hết hàng
            String sqlStock = "SELECT COUNT(*) FROM Inventory WHERE Quantity <= MinRequired";
            ps = conn.prepareStatement(sqlStock);
            rs = ps.executeQuery();
            if (rs.next()) stats.setLowStockCount(rs.getInt(1));
            rs.close(); ps.close();

            // 4. Lấy danh sách 5 đơn hàng gần nhất
            String sqlRecent = "SELECT TOP 5 o.OrderID, CONVERT(VARCHAR(5), o.OrderDate, 108) AS TimeOrder, t.TableName, o.FinalAmount, o.OrderStatus " +
                               "FROM Orders o JOIN Tables t ON o.TableID = t.TableID ORDER BY o.OrderDate DESC";
            ps = conn.prepareStatement(sqlRecent);
            rs = ps.executeQuery();
            while (rs.next()) {
                OrderSummary os = new OrderSummary();
                os.setOrderId(rs.getInt("OrderID"));
                os.setTimeOrder(rs.getString("TimeOrder"));
                os.setTableName(rs.getString("TableName"));
                os.setFinalAmount(rs.getDouble("FinalAmount"));
                os.setOrderStatus(rs.getString("OrderStatus"));
                recentOrders.add(os);
            }
            stats.setRecentOrders(recentOrders);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); if (ps != null) ps.close(); if (conn != null) conn.close(); } catch (SQLException e) {}
        }
        return stats;
    }
}
