package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.CartItem;
import com.mycoffee.model.Order;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public int createCustomerOrder(int branchId, int tableId, Integer customerId, List<CartItem> cart, String note) {
        if (cart == null || cart.isEmpty()) {
            return -1;
        }

        int orderId = -1;
        double totalAmount = 0;
        for (CartItem item : cart) {
            totalAmount += item.getLineTotal();
        }

        String insertOrderSql = "INSERT INTO Orders "
                + "(BranchID, TableID, CustomerID, CashierID, OrderType, TotalAmount, DiscountAmount, FinalAmount, OrderStatus, OrderDate) "
                + "VALUES (?, ?, CASE WHEN EXISTS (SELECT 1 FROM Customers WHERE CustomerID = ?) THEN ? ELSE NULL END, "
                + "NULL, N'Eat-in', ?, 0, ?, N'Pending', GETDATE())";
        String insertDetailSql = "INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, Note, ItemStatus) "
                + "VALUES (?, ?, ?, ?, ?, N'Pending')";
        String updateTableSql = "UPDATE Tables SET Status = N'Occupied' WHERE TableID = ?";

        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                psOrder.setInt(1, branchId);
                psOrder.setInt(2, tableId);
                psOrder.setInt(3, customerId != null ? customerId : -1);
                psOrder.setInt(4, customerId != null ? customerId : -1);
                psOrder.setDouble(5, totalAmount);
                psOrder.setDouble(6, totalAmount);
                psOrder.executeUpdate();

                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }

            if (orderId <= 0) {
                conn.rollback();
                return -1;
            }

            try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSql)) {
                for (CartItem item : cart) {
                    Product product = item.getProduct();
                    if (product == null || item.getQuantity() <= 0) {
                        continue;
                    }
                    psDetail.setInt(1, orderId);
                    psDetail.setInt(2, product.getProductId());
                    psDetail.setInt(3, item.getQuantity());
                    psDetail.setDouble(4, product.getBasePrice());
                    psDetail.setString(5, note);
                    psDetail.addBatch();
                }
                psDetail.executeBatch();
            }

            try (PreparedStatement psTable = conn.prepareStatement(updateTableSql)) {
                psTable.setInt(1, tableId);
                psTable.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return orderId;
    }

    public int createNewOrder(int branchId, int tableId, int cashierId) {
        int orderId = -1;
        String insertOrderSql = "INSERT INTO Orders (BranchID, TableID, CashierID, OrderType, OrderStatus, OrderDate) VALUES (?, ?, ?, 'Dine-in', 'Pending', GETDATE())";
        String updateTableSql = "UPDATE Tables SET Status = 'Occupied' WHERE TableID = ?";

        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                psOrder.setInt(1, branchId);
                psOrder.setInt(2, tableId);
                psOrder.setInt(3, cashierId);
                psOrder.executeUpdate();
                try (ResultSet rs = psOrder.getGeneratedKeys()) {
                    if (rs.next()) orderId = rs.getInt(1);
                }
            }
            try (PreparedStatement psTable = conn.prepareStatement(updateTableSql)) {
                psTable.setInt(1, tableId);
                psTable.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
        return orderId;
    }

    public void addOrUpdateOrderDetail(int orderId, int productId, int quantity, double unitPrice) {
        String checkSql = "SELECT Quantity FROM OrderDetails WHERE OrderID = ? AND ProductID = ?";
        String insertSql = "INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, ItemStatus) VALUES (?, ?, ?, ?, 'Pending')";
        String updateSql = "UPDATE OrderDetails SET Quantity = Quantity + ? WHERE OrderID = ? AND ProductID = ?";
        String deleteSql = "DELETE FROM OrderDetails WHERE OrderID = ? AND ProductID = ?";
        String updateTotalOrderSql = "UPDATE Orders SET TotalAmount = (SELECT COALESCE(SUM(Quantity * UnitPrice), 0) FROM OrderDetails WHERE OrderID = ?), DiscountAmount = 0, FinalAmount = (SELECT COALESCE(SUM(Quantity * UnitPrice), 0) FROM OrderDetails WHERE OrderID = ?) WHERE OrderID = ?";

        try (Connection conn = new DBContext().getConnection()) {
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

            if (exists && (currentQty + quantity <= 0)) {
                try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
                    psDelete.setInt(1, orderId);
                    psDelete.setInt(2, productId);
                    psDelete.executeUpdate();
                }
            } else if (exists) {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, quantity);
                    psUpdate.setInt(2, orderId);
                    psUpdate.setInt(3, productId);
                    psUpdate.executeUpdate();
                }
            } else if (quantity > 0) {
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, orderId);
                    psInsert.setInt(2, productId);
                    psInsert.setInt(3, quantity);
                    psInsert.setDouble(4, unitPrice);
                    psInsert.executeUpdate();
                }
            }

            try (PreparedStatement psUpdateTotal = conn.prepareStatement(updateTotalOrderSql)) {
                psUpdateTotal.setInt(1, orderId);
                psUpdateTotal.setInt(2, orderId);
                psUpdateTotal.setInt(3, orderId);
                psUpdateTotal.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateOrderDiscount(int orderId, double discountAmount) {
        String updateSql = "UPDATE Orders SET DiscountAmount = ?, FinalAmount = TotalAmount - ? WHERE OrderID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setDouble(1, discountAmount);
            ps.setDouble(2, discountAmount);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, t.TableName FROM Orders o JOIN Tables t ON o.TableID = t.TableID WHERE o.OrderID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("OrderID"));
                    order.setTableId(rs.getInt("TableID"));
                    order.setBranchId(rs.getInt("BranchID"));
                    order.setCashierId(rs.getInt("CashierID"));
                    order.setTotalAmount(rs.getDouble("TotalAmount"));
                    order.setDiscountAmount(rs.getDouble("DiscountAmount"));
                    order.setFinalAmount(rs.getDouble("FinalAmount"));
                    order.setOrderStatus(rs.getString("OrderStatus"));
                    order.setOrderDate(rs.getTimestamp("OrderDate"));
                    order.setOrderType(rs.getString("TableName"));
                    return order;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<Order> getOrdersByCustomerId(int customerId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, t.TableName FROM Orders o "
                + "JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE o.CustomerID = ? "
                + "ORDER BY o.OrderDate DESC, o.OrderID DESC";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getInt("OrderID"));
                    order.setBranchId(rs.getInt("BranchID"));
                    order.setTableId(rs.getInt("TableID"));
                    order.setCashierId(rs.getInt("CashierID"));
                    order.setTotalAmount(rs.getDouble("TotalAmount"));
                    order.setDiscountAmount(rs.getDouble("DiscountAmount"));
                    order.setFinalAmount(rs.getDouble("FinalAmount"));
                    order.setOrderStatus(rs.getString("OrderStatus"));
                    order.setOrderDate(rs.getTimestamp("OrderDate"));
                    order.setOrderType(rs.getString("TableName"));
                    list.add(order);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

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
                    od.setNote(rs.getString("ProductName"));
                    od.setItemStatus(rs.getString("ItemStatus"));
                    list.add(od);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public int getPendingOrderIdByTable(int tableId) {
        String sql = "SELECT OrderID FROM Orders WHERE TableID = ? AND OrderStatus = 'Pending'";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("OrderID");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public void cancelOrder(int orderId, int tableId) {
        String updateOrder = "UPDATE Orders SET OrderStatus = 'Cancelled' WHERE OrderID = ?";
        String updateTable = "UPDATE Tables SET Status = 'Empty' WHERE TableID = ?";
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateOrder)) {
                ps1.setInt(1, orderId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(updateTable)) {
                ps2.setInt(1, tableId);
                ps2.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void resetAllTables(int branchId) {
        String cancelOrders = "UPDATE Orders SET OrderStatus = 'Cancelled' WHERE BranchID = ? AND OrderStatus = 'Pending'";
        String resetTables = "UPDATE Tables SET Status = 'Empty' WHERE BranchID = ?";
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(cancelOrders)) {
                ps1.setInt(1, branchId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(resetTables)) {
                ps2.setInt(1, branchId);
                ps2.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void completeOrder(int orderId) {
        String updateOrder = "UPDATE Orders SET OrderStatus = 'Completed' WHERE OrderID = ?";
        String updateTable = "UPDATE Tables SET Status = 'Empty' WHERE TableID = (SELECT TableID FROM Orders WHERE OrderID = ?)";
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateOrder)) {
                ps1.setInt(1, orderId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(updateTable)) {
                ps2.setInt(1, orderId);
                ps2.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
