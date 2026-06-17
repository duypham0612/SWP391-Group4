package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Dashboard;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardDAO extends DBContext {

    public Dashboard getDashboardStats() {
        Dashboard dto = new Dashboard();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection(); // Gọi hàm kết nối từ DBContext của bạn
            
            // 1. Tính tổng doanh thu từ các đơn hàng thành công
            String sqlRevenue = "SELECT SUM(FinalAmount) AS Total FROM Orders WHERE OrderStatus = 'Completed'";
            ps = conn.prepareStatement(sqlRevenue);
            rs = ps.executeQuery();
            if (rs.next()) {
                dto.setTotalRevenue(rs.getDouble("Total"));
            }
            ps.close(); // Giải phóng lệnh cũ để tái sử dụng biến ps
            
            // 2. Tính tổng số đơn hàng đang có trong hệ thống
            String sqlOrders = "SELECT COUNT(OrderID) AS TotalOrders FROM Orders";
            ps = conn.prepareStatement(sqlOrders);
            rs = ps.executeQuery();
            if (rs.next()) {
                dto.setTotalOrders(rs.getInt("TotalOrders"));
            }
            ps.close();
            
            // 3. Đếm số lượng nguyên liệu có Quantity chạm hoặc dưới mức MinRequired
            String sqlStock = "SELECT COUNT(*) AS LowStock FROM Inventory WHERE Quantity <= MinRequired";
            ps = conn.prepareStatement(sqlStock);
            rs = ps.executeQuery();
            if (rs.next()) {
                dto.setLowStockCount(rs.getInt("LowStock"));
            }
            ps.close();
            
            // 4. Đếm số tài khoản nhân viên đang kích hoạt (loại trừ Khách hàng RoleID = 5)
            String sqlEmployees = "SELECT COUNT(*) AS ActiveEmp FROM Users WHERE IsActive = 1 AND RoleID != 5";
            ps = conn.prepareStatement(sqlEmployees);
            rs = ps.executeQuery();
            if (rs.next()) {
                dto.setActiveEmployees(rs.getInt("ActiveEmp"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Đóng tất cả kết nối an toàn
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return dto;
    }
}
