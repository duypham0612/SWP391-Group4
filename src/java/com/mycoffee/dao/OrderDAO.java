package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.Order;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // Chức năng 2: Tạo Order mới khi click vào bàn trống
    public int createNewOrder(int branchId, int tableId, int cashierId) {
        int orderId = -1;
        String insertOrderSql = "INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, OrderStatus, OrderDate) VALUES (?, ?, ?, 'Dine-in', 'Pending', GETDATE())";
        String updateTableSql = "UPDATE Tables SET Status = 'Occupied' WHERE TableID = ?";

        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false); // Bật Transaction để đảm bảo an toàn dữ liệu

            // 1. Tạo Order mới và lấy về OrderID vừa tạo
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                psOrder.setInt(1, branchId);
                psOrder.setInt(2, tableId);
                psOrder.setInt(3, cashierId);
                psOrder.executeUpdate();

                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }

            // 2. Cập nhật trạng thái bàn thành Đang sử dụng (Occupied)
            try (PreparedStatement psTable = conn.prepareStatement(updateTableSql)) {
                psTable.setInt(1, tableId);
                psTable.executeUpdate();
            }

            conn.commit(); // Hoàn tất Transaction
        } catch (Exception e) {
            System.out.println("Lỗi tạo order: " + e.getMessage());
        }
        return orderId;
    }

    // Chức năng 3: Thêm hoặc cập nhật số lượng món ăn trong Order
    public void addOrUpdateOrderDetail(int orderId, int productId, int quantity, double unitPrice) {
        String checkSql = "SELECT Quantity FROM OrderDetails WHERE OrderID = ? AND ProductID = ?";
        String insertSql = "INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, ItemStatus) VALUES (?, ?, ?, ?, 'Pending')";
        String updateSql = "UPDATE OrderDetails SET Quantity = Quantity + ? WHERE OrderID = ? AND ProductID = ?";
        String deleteSql = "DELETE FROM OrderDetails WHERE OrderID = ? AND ProductID = ?";
        String updateTotalOrderSql = "UPDATE Orders SET TotalAmount = (SELECT COALESCE(SUM(Quantity * UnitPrice), 0) FROM OrderDetails WHERE OrderID = ?), FinalAmount = (SELECT COALESCE(SUM(Quantity * UnitPrice), 0) FROM OrderDetails WHERE OrderID = ?) WHERE OrderID = ?";

        try (Connection conn = new DBContext().getConnection()) {
            // Kiểm tra xem món này đã có trong Order chưa
            boolean exists = false;
            int currentQty = 0;
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, orderId);
                psCheck.setInt(2, productId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        currentQty = rs.getInt("Quantity");
                    }
                }
            }

            // Nếu Số lượng cộng thêm bị âm và tổng qty <= 0 -> Xóa món khỏi Order
            if (exists && (currentQty + quantity <= 0)) {
                try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                    psDelete.setInt(1, orderId);
                    psDelete.setInt(2, productId);
                    psDelete.executeUpdate();
                }
            } else if (exists) {
                // Đã có -> Cập nhật cộng/trừ dồn số lượng
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, quantity);
                    psUpdate.setInt(2, orderId);
                    psUpdate.setInt(3, productId);
                    psUpdate.executeUpdate();
                }
            } else if (quantity > 0) {
                // Chưa có -> Thêm mới vào Order
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, orderId);
                    psInsert.setInt(2, productId);
                    psInsert.setInt(3, quantity);
                    psInsert.setDouble(4, unitPrice);
                    psInsert.executeUpdate();
                }
            }

            // Cập nhật lại Tổng tiền (TotalAmount) của đơn hàng
            try (PreparedStatement psUpdateTotal = conn.prepareStatement(updateTotalOrderSql)) {
                psUpdateTotal.setInt(1, orderId);
                psUpdateTotal.setInt(2, orderId);
                psUpdateTotal.setInt(3, orderId);
                psUpdateTotal.executeUpdate();
            }

        } catch (Exception e) {
            System.out.println("Lỗi thêm/xóa món: " + e.getMessage());
        }
    }

    // Lấy thông tin tổng quan của Order (kèm Tên Bàn)
    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, t.TableName FROM Orders o JOIN Tables t ON o.TableID = t.TableID WHERE o.OrderID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("OrderID"));
                    order.setTotalAmount(rs.getDouble("TotalAmount"));
                    // Tạm dùng OrderType chứa TableName để đẩy ra UI cho gọn
                    order.setOrderType(rs.getString("TableName"));
                    return order;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // Lấy danh sách các món đã thêm vào Order
    public List<OrderDetail> getOrderDetails(int orderId) {
        List<OrderDetail> list = new ArrayList<>();
        String sql = "SELECT od.*, p.ProductName FROM OrderDetails od JOIN Products p ON od.ProductID = p.ProductID WHERE od.OrderID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail od = new OrderDetail();
                    od.setOrderDetailId(rs.getInt("OrderDetailID"));
                    od.setProductId(rs.getInt("ProductID"));
                    od.setQuantity(rs.getInt("Quantity"));
                    od.setUnitPrice(rs.getDouble("UnitPrice"));
                    // Tạm mượn Note để chứa ProductName hiển thị ra View
                    od.setNote(rs.getString("ProductName"));
                    list.add(od);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}