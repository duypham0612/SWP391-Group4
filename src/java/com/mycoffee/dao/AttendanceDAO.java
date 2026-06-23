package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Attendance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    // Lấy danh sách chấm công của ngày hôm nay theo Chi nhánh
    public List<Attendance> getTodayAttendanceByBranch(int branchId) {
        List<Attendance> list = new ArrayList<>();
        // Câu lệnh SQL lấy dữ liệu chấm công ngày hôm nay, kết hợp JOIN để lấy Tên nhân viên và Tên Ca làm việc
        String sql = "SELECT a.AttendanceID, a.EmployeeID, a.ShiftID, a.Date, a.CheckInTime, "
                   + "       u.FullName AS EmployeeName, s.ShiftName "
                   + "FROM Attendance a "
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
}