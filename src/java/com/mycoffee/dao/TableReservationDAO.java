package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Table;
import com.mycoffee.model.Reservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TableReservationDAO {

    // Hàm bổ trợ để lấy kết nối an toàn từ DBContext, giải quyết lỗi non-static và Exception
    private Connection getConnection() throws Exception {
        DBContext db = new DBContext();
        return db.getConnection();
    }

    // 1. Lấy danh sách tất cả các bàn thuộc một chi nhánh (BranchID)
    // Lấy danh sách tất cả các bàn dựa theo BranchID (Đã cập nhật lấy thêm cột Capacity)
    public List<Table> getTablesByBranch(int branchId) {
        List<Table> list = new ArrayList<>();
        // THÊM: Bổ sung [Capacity] vào câu lệnh SELECT
        String sql = "SELECT [TableID], [BranchID], [TableName], [QRCodeURL], [Status], [Capacity] "
                   + "FROM [Tables] WHERE [BranchID] = ? ORDER BY [TableName] ASC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Table t = new Table();
                    t.setTableID(rs.getInt("TableID"));
                    t.setBranchID(rs.getInt("BranchID"));
                    t.setTableName(rs.getString("TableName"));
                    t.setQrCodeURL(rs.getString("QRCodeURL"));
                    t.setStatus(rs.getString("Status"));
                    t.setCapacity(rs.getInt("Capacity")); // THÊM MỚI DÒNG NÀY ĐỂ ĐỌC SỨC CHỨA
                    list.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. GIẢI QUYẾT LỖI DÒNG 41 CONTROLLER: Lấy danh sách đặt bàn ngày hôm nay của chi nhánh
    public List<Reservation> getTodayReservations(int branchId) {
        List<Reservation> list = new ArrayList<>();
        // Ánh xạ lọc các bản ghi từ bảng Orders có loại hình dịch vụ phục vụ tại quán (Eat-in) trong ngày hôm nay
        String sql = "SELECT [OrderID], [BranchID], [TableID], [OrderType], [OrderStatus], [OrderDate] "
                   + "FROM [Orders] "
                   + "WHERE [BranchID] = ? "
                   + "AND [OrderType] = 'Eat-in' "
                   + "AND CAST([OrderDate] AS DATE) = CAST(GETDATE() AS DATE) "
                   + "AND [OrderStatus] IN ('Pending', 'Confirmed') "
                   + "ORDER BY [OrderDate] ASC";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation res = new Reservation();
                    // Gán tạm mã OrderID vào thuộc tính ReservationID để Controller lấy ra xử lý đổi trạng thái
                    res.setReservationID(rs.getInt("OrderID"));
                    res.setBranchID(rs.getInt("BranchID"));
                    res.setTableID(rs.getInt("TableID"));
                    res.setReservationTime(rs.getTimestamp("OrderDate"));
                    res.setStatus(rs.getString("OrderStatus"));
                    
                    // Các trường thông tin bổ sung mặc định cho giao diện
                    res.setCustomerName("Khách Hàng Đặt Trước");
                    res.setPhone("");
                    list.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. Cập nhật trạng thái của Bàn ăn (Ví dụ: Empty, Reserved, Serving)
    public boolean updateTableStatus(int tableId, String status) {
        String sql = "UPDATE [Tables] SET [Status] = ? WHERE [TableID] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, status);
            ps.setInt(2, tableId);
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. GIẢI QUYẾT LỖI DÒNG 73 CONTROLLER: Tạo lịch đặt hẹn giữ bàn mới
    public boolean createNewReservation(Reservation res) {
        String sql = "INSERT INTO [Orders] ([BranchID], [TableID], [OrderType], [OrderStatus], [OrderDate], [TotalAmount], [DiscountAmount], [FinalAmount]) "
                   + "VALUES (?, ?, ?, ?, ?, 0, 0, 0)";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (res.getBranchID() != null) ps.setInt(1, res.getBranchID()); else ps.setNull(1, java.sql.Types.INTEGER);
            if (res.getTableID() != null) ps.setInt(2, res.getTableID()); else ps.setNull(2, java.sql.Types.INTEGER);
            
            ps.setString(3, "Eat-in"); 
            ps.setString(4, res.getStatus() != null ? res.getStatus() : "Pending");
            ps.setTimestamp(5, res.getReservationTime());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. GIẢI QUYẾT LỖI DÒNG 81 CONTROLLER: Cập nhật trạng thái của phiếu hẹn từ Đang chờ sang Đã ngồi ăn
    public boolean updateReservationStatus(int resId, String status) {
        String sql = "UPDATE [Orders] SET [OrderStatus] = ? WHERE [OrderID] = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, status);
            ps.setInt(2, resId);
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}