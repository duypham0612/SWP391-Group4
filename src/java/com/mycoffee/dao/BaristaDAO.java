package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.BaristaQueueItem;
import com.mycoffee.model.BaristaProductStat;
import com.mycoffee.model.ProductAvailability;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO phục vụ màn hình Barista (Pha chế).
 * Làm việc chủ yếu trên cột OrderDetails.ItemStatus
 * (Pending -> Preparing -> Completed, hoặc OutOfStock khi báo hết món).
 */
public class BaristaDAO {

    // Các cột dùng chung cho việc map sang BaristaQueueItem
    private static final String SELECT_COLS =
            "SELECT od.OrderDetailID, od.OrderID, od.ProductID, p.ProductName, "
          + "od.Quantity, od.Note, od.ItemStatus, od.StartedAt, od.CompletedAt, "
          + "o.OrderDate, o.Priority, t.TableName ";

    private BaristaQueueItem map(ResultSet rs) throws Exception {
        BaristaQueueItem item = new BaristaQueueItem();
        item.setOrderDetailId(rs.getInt("OrderDetailID"));
        item.setOrderId(rs.getInt("OrderID"));
        item.setProductId(rs.getInt("ProductID"));
        item.setProductName(rs.getString("ProductName"));
        item.setQuantity(rs.getInt("Quantity"));
        item.setNote(rs.getString("Note"));
        item.setItemStatus(rs.getString("ItemStatus"));
        item.setStartedAt(rs.getTimestamp("StartedAt"));
        item.setCompletedAt(rs.getTimestamp("CompletedAt"));
        item.setOrderDate(rs.getTimestamp("OrderDate"));
        item.setPriority(rs.getInt("Priority"));
        item.setTableName(rs.getString("TableName"));
        return item;
    }

    /**
     * Lấy hàng chờ pha chế trong ngày hôm nay của 1 chi nhánh:
     * gồm các món Pending / Preparing / Completed thuộc order chưa bị huỷ.
     * Sắp xếp: order được ghim ưu tiên lên trước, rồi tới order gọi sớm nhất.
     */
    public List<BaristaQueueItem> getQueue(int branchId) {
        List<BaristaQueueItem> list = new ArrayList<>();
        String sql = SELECT_COLS
                + "FROM OrderDetails od "
                + "JOIN Orders o ON od.OrderID = o.OrderID "
                + "JOIN Products p ON od.ProductID = p.ProductID "
                + "LEFT JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE o.BranchID = ? "
                + "AND o.OrderStatus <> 'Cancelled' "
                + "AND od.ItemStatus IN ('Pending', 'Preparing', 'Completed') "
                + "AND CAST(o.OrderDate AS DATE) = CAST(GETDATE() AS DATE) "
                + "ORDER BY o.Priority DESC, o.OrderDate ASC, od.OrderDetailID ASC";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Cập nhật trạng thái pha chế của 1 món + ghi nhận mốc thời gian.
     * - Preparing: ghi StartedAt (nếu chưa có)
     * - Completed: ghi CompletedAt, đảm bảo có StartedAt
     * - Pending: reset lại các mốc thời gian
     */
    public boolean updateItemStatus(int orderDetailId, String newStatus) {
        String sql;
        if ("Preparing".equals(newStatus)) {
            sql = "UPDATE OrderDetails SET ItemStatus = 'Preparing', "
                + "StartedAt = ISNULL(StartedAt, GETDATE()), CompletedAt = NULL "
                + "WHERE OrderDetailID = ?";
        } else if ("Completed".equals(newStatus)) {
            sql = "UPDATE OrderDetails SET ItemStatus = 'Completed', "
                + "StartedAt = ISNULL(StartedAt, GETDATE()), CompletedAt = GETDATE() "
                + "WHERE OrderDetailID = ?";
        } else if ("Pending".equals(newStatus)) {
            sql = "UPDATE OrderDetails SET ItemStatus = 'Pending', "
                + "StartedAt = NULL, CompletedAt = NULL "
                + "WHERE OrderDetailID = ?";
        } else {
            // Trạng thái khác (vd OutOfStock) -> chỉ đổi cờ trạng thái
            sql = "UPDATE OrderDetails SET ItemStatus = ? WHERE OrderDetailID = ?";
        }

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (sql.startsWith("UPDATE OrderDetails SET ItemStatus = ? ")) {
                ps.setString(1, newStatus);
                ps.setInt(2, orderDetailId);
            } else {
                ps.setInt(1, orderDetailId);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Báo món tạm hết: tắt sản phẩm khỏi menu (IsAvailable = 0)
     * và đánh dấu các món cùng loại còn trong hàng chờ là OutOfStock.
     */
    public boolean markProductOutOfStock(int productId) {
        String offMenu = "UPDATE Products SET IsAvailable = 0 WHERE ProductID = ?";
        String flagItems = "UPDATE OrderDetails SET ItemStatus = 'OutOfStock' "
                         + "WHERE ProductID = ? AND ItemStatus IN ('Pending', 'Preparing')";
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(offMenu)) {
                ps1.setInt(1, productId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(flagItems)) {
                ps2.setInt(1, productId);
                ps2.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Ghim / bỏ ghim ưu tiên cho 1 order.
     * Nếu đang ghim (Priority > 0) thì bỏ ghim (về 0);
     * ngược lại đẩy lên cao nhất chi nhánh (MAX + 1).
     */
    public boolean togglePriority(int orderId) {
        String sql = "UPDATE Orders SET Priority = "
                + "CASE WHEN Priority > 0 THEN 0 "
                + "ELSE (SELECT ISNULL(MAX(o2.Priority), 0) + 1 FROM Orders o2 "
                + "      WHERE o2.BranchID = Orders.BranchID) END "
                + "WHERE OrderID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lịch sử các món đã pha xong (Completed) theo ngày.
     * dateStr định dạng yyyy-MM-dd; null -> lấy hôm nay.
     */
    public List<BaristaQueueItem> getHistory(int branchId, String dateStr) {
        List<BaristaQueueItem> list = new ArrayList<>();
        String sql = SELECT_COLS
                + "FROM OrderDetails od "
                + "JOIN Orders o ON od.OrderID = o.OrderID "
                + "JOIN Products p ON od.ProductID = p.ProductID "
                + "LEFT JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE o.BranchID = ? "
                + "AND od.ItemStatus = 'Completed' "
                + "AND CAST(od.CompletedAt AS DATE) = "
                + (dateStr == null ? "CAST(GETDATE() AS DATE) " : "? ")
                + "ORDER BY od.CompletedAt DESC";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            if (dateStr != null) {
                ps.setString(2, dateStr);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Lấy thông tin 1 món để in tem ly. */
    public BaristaQueueItem getLabelItem(int orderDetailId) {
        String sql = SELECT_COLS
                + "FROM OrderDetails od "
                + "JOIN Orders o ON od.OrderID = o.OrderID "
                + "JOIN Products p ON od.ProductID = p.ProductID "
                + "LEFT JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE od.OrderDetailID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderDetailId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Tất cả món của 1 order (cho màn Chi tiết order). */
    public List<BaristaQueueItem> getOrderItems(int orderId) {
        List<BaristaQueueItem> list = new ArrayList<>();
        String sql = SELECT_COLS
                + "FROM OrderDetails od "
                + "JOIN Orders o ON od.OrderID = o.OrderID "
                + "JOIN Products p ON od.ProductID = p.ProductID "
                + "LEFT JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE od.OrderID = ? ORDER BY od.OrderDetailID";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Các món đã hoàn thành hôm nay - dùng cho màn gọi món (Pickup display). */
    public List<BaristaQueueItem> getReadyForPickup(int branchId) {
        List<BaristaQueueItem> list = new ArrayList<>();
        String sql = SELECT_COLS
                + "FROM OrderDetails od "
                + "JOIN Orders o ON od.OrderID = o.OrderID "
                + "JOIN Products p ON od.ProductID = p.ProductID "
                + "LEFT JOIN Tables t ON o.TableID = t.TableID "
                + "WHERE o.BranchID = ? AND od.ItemStatus = 'Completed' "
                + "AND CAST(od.CompletedAt AS DATE) = CAST(GETDATE() AS DATE) "
                + "ORDER BY od.CompletedAt DESC";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Đếm số món theo trạng thái trong ngày: [Pending, Preparing, Completed]. */
    public int[] getDashboardCounts(int branchId) {
        int[] r = new int[3];
        String sql = "SELECT "
                + "SUM(CASE WHEN od.ItemStatus='Pending' THEN 1 ELSE 0 END) p, "
                + "SUM(CASE WHEN od.ItemStatus='Preparing' THEN 1 ELSE 0 END) pr, "
                + "SUM(CASE WHEN od.ItemStatus='Completed' THEN 1 ELSE 0 END) c "
                + "FROM OrderDetails od JOIN Orders o ON od.OrderID=o.OrderID "
                + "WHERE o.BranchID=? AND o.OrderStatus<>'Cancelled' "
                + "AND CAST(o.OrderDate AS DATE)=CAST(GETDATE() AS DATE)";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    r[0] = rs.getInt("p");
                    r[1] = rs.getInt("pr");
                    r[2] = rs.getInt("c");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    /** Thời gian pha trung bình (giây) của các món hoàn thành hôm nay. */
    public int getAvgPrepSecondsToday(int branchId) {
        String sql = "SELECT AVG(DATEDIFF(SECOND, od.StartedAt, od.CompletedAt)) avg_sec "
                + "FROM OrderDetails od JOIN Orders o ON od.OrderID=o.OrderID "
                + "WHERE o.BranchID=? AND od.ItemStatus='Completed' "
                + "AND od.StartedAt IS NOT NULL AND od.CompletedAt IS NOT NULL "
                + "AND CAST(od.CompletedAt AS DATE)=CAST(GETDATE() AS DATE)";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("avg_sec");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Thống kê pha chế theo món trong khoảng ngày (yyyy-MM-dd).
     * from/to null -> mặc định hôm nay.
     */
    public List<BaristaProductStat> getProductStats(int branchId, String fromDate, String toDate) {
        List<BaristaProductStat> list = new ArrayList<>();
        String fromExpr = (fromDate == null) ? "CAST(GETDATE() AS DATE)" : "?";
        String toExpr = (toDate == null) ? "CAST(GETDATE() AS DATE)" : "?";
        String sql = "SELECT p.ProductID, p.ProductName, "
                + "SUM(od.Quantity) totalQty, COUNT(*) orderCount, "
                + "AVG(CASE WHEN od.StartedAt IS NOT NULL AND od.CompletedAt IS NOT NULL "
                + "    THEN DATEDIFF(SECOND, od.StartedAt, od.CompletedAt) END) avgPrep "
                + "FROM OrderDetails od JOIN Orders o ON od.OrderID=o.OrderID "
                + "JOIN Products p ON od.ProductID=p.ProductID "
                + "WHERE o.BranchID=? AND od.ItemStatus='Completed' "
                + "AND CAST(od.CompletedAt AS DATE) BETWEEN " + fromExpr + " AND " + toExpr + " "
                + "GROUP BY p.ProductID, p.ProductName ORDER BY totalQty DESC";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            if (fromDate != null) ps.setString(idx++, fromDate);
            if (toDate != null) ps.setString(idx++, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BaristaProductStat s = new BaristaProductStat();
                    s.setProductId(rs.getInt("ProductID"));
                    s.setProductName(rs.getString("ProductName"));
                    s.setTotalQty(rs.getInt("totalQty"));
                    s.setOrderCount(rs.getInt("orderCount"));
                    s.setAvgPrepSeconds(rs.getInt("avgPrep"));
                    list.add(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Tất cả sản phẩm + trạng thái còn bán (màn Quản lý sản phẩm / Báo hết món). */
    public List<ProductAvailability> getAllProductsAvailability() {
        List<ProductAvailability> list = new ArrayList<>();
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, c.CategoryName, "
                + "CAST(ISNULL(p.IsAvailable,0) AS INT) av "
                + "FROM Products p LEFT JOIN Categories c ON p.CategoryID=c.CategoryID "
                + "ORDER BY c.CategoryName, p.ProductName";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ProductAvailability pa = new ProductAvailability();
                pa.setProductId(rs.getInt("ProductID"));
                pa.setProductName(rs.getString("ProductName"));
                pa.setBasePrice(rs.getDouble("BasePrice"));
                pa.setCategoryName(rs.getString("CategoryName"));
                pa.setAvailable(rs.getInt("av") == 1);
                list.add(pa);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Bật/tắt trạng thái còn bán của 1 sản phẩm. */
    public boolean setProductAvailable(int productId, boolean available) {
        String sql = "UPDATE Products SET IsAvailable = ? WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
