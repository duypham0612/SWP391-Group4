package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Attendance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    // 1. READ: Lấy danh sách chấm công của ngày hôm nay theo Chi nhánh
    public List<Attendance> getTodayAttendanceByBranch(int branchId) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT a.AttendanceID, a.EmployeeID, a.ShiftID, a.Date, a.CheckInTime, a.CheckOutTime, a.Status, "
                   + "       u.FullName AS EmployeeName, s.ShiftName "
                   + "FROM Attendances a " 
                   + "JOIN Employees e ON a.EmployeeID = e.EmployeeID "
                   + "JOIN Users u ON e.EmployeeID = u.UserID "
                   + "JOIN Shifts s ON a.ShiftID = s.ShiftID "
                   + "WHERE e.BranchID = ? AND a.Date = CAST(GETDATE() AS DATE)";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setAttendanceId(rs.getInt("AttendanceID"));
                    att.setEmployeeId(rs.getInt("EmployeeID"));
                    att.setShiftId(rs.getInt("ShiftID"));
                    att.setDate(rs.getDate("Date"));
                    att.setCheckInTime(rs.getTimestamp("CheckInTime"));
                    att.setCheckOutTime(rs.getTimestamp("CheckOutTime"));
                    att.setStatus(rs.getString("Status"));
                    att.setEmployeeName(rs.getString("EmployeeName"));
                    att.setShiftName(rs.getString("ShiftName"));
                    list.add(att);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham getTodayAttendanceByBranch: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 2. CREATE: Thêm mới một lượt chấm công (Sử dụng try-with-resources để tự động đóng kết nối an toàn)
    public void insertAttendance(int empId, int shiftId, String status) {
        String sql = "INSERT INTO [dbo].[Attendances] ([EmployeeID], [ShiftID], [Date], [CheckInTime], [Status]) "
                   + "VALUES (?, ?, CAST(GETDATE() AS DATE), GETDATE(), ?)";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, shiftId);
            ps.setString(3, status);

            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Loi insertAttendance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 3. UPDATE (Trường hợp 1): Nhân viên tự bấm nút "Check-out" để kết thúc ca
    public boolean checkOut(int attendanceId) {
        String sql = "UPDATE Attendances "
                   + "SET CheckOutTime = GETDATE(), Status = N'Đã hoàn thành' "
                   + "WHERE AttendanceID = ?";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham checkOut: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 3. UPDATE (Trường hợp 2): Admin chủ động sửa trạng thái từ Modal 
    // TỐI ƯU: Nếu chuyển sang 'Đã hoàn thành' thì tự cập nhật Giờ ra, ngược lại chuyển về Đang làm việc/Đi muộn/Vắng mặt thì xóa/giữ nguyên Giờ ra hợp lý.
    public boolean updateStatus(int attendanceId, String status) {
        String sql = "UPDATE Attendances "
                   + "SET Status = ?, "
                   + "    CheckOutTime = CASE WHEN ? = N'Đã hoàn thành' THEN GETDATE() ELSE NULL END "
                   + "WHERE AttendanceID = ?";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, attendanceId);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham updateStatus: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 4. DELETE: Xóa hẳn bản ghi chấm công bị lỗi do bấm nhầm
    public boolean deleteAttendance(int attendanceId) {
        String sql = "DELETE FROM Attendances WHERE AttendanceID = ?";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendanceId);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham deleteAttendance: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}