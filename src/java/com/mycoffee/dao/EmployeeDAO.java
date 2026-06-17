package com.mycoffee.dao;

import com.mycoffee.model.Employee;
import com.mycoffee.model.Role;
import com.mycoffee.context.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    // Hàm kết nối Cơ sở dữ liệu chung
    private Connection getConnection() throws Exception {
        return new DBContext().getConnection();
    }

    // 1. LẤY TẤT CẢ NHÂN VIÊN (Bổ sung Username, Phone, RoleID)
    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        // Đã THÊM: u.Username, u.Phone, u.RoleID vào câu SQL
        String sql = "SELECT u.UserID, u.Username, u.FullName, u.Email, u.Phone, u.RoleID, r.RoleName, u.IsActive, u.ShiftID "
                + "FROM Users u "
                + "JOIN Roles r ON u.RoleID = r.RoleID "
                + "WHERE r.RoleName != 'Customer'";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("UserID");
                String username = rs.getString("Username"); // Mới bổ sung
                String name = rs.getString("FullName");
                String email = rs.getString("Email");
                String phone = rs.getString("Phone");       // Mới bổ sung
                int roleId = rs.getInt("RoleID");           // Mới bổ sung
                String role = rs.getString("RoleName");
                boolean isActive = rs.getBoolean("IsActive");
                int shiftId = rs.getInt("ShiftID");

                String status = isActive ? "Đang làm" : "Nghỉ việc";
                String shift = "Chưa xếp ca";

                // Khởi tạo đối tượng dựa trên Constructor đầy đủ thuộc tính mới
                Employee emp = new Employee(id, username, null, name, email, phone, roleId, role, status, shift, shiftId);
                list.add(emp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. LẤY THÔNG TIN 1 NHÂN VIÊN THEO ID (Bổ sung các trường để điền vào Form Edit)
    public Employee getEmployeeById(int id) {
        // Đã THÊM: u.Username, u.Phone, u.RoleID vào câu SQL
        String sql = "SELECT u.UserID, u.Username, u.FullName, u.Email, u.Phone, u.RoleID, r.RoleName, u.IsActive, u.ShiftID "
                + "FROM Users u "
                + "JOIN Roles r ON u.RoleID = r.RoleID "
                + "WHERE u.UserID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Employee(
                            rs.getInt("UserID"),
                            rs.getString("Username"), // Ánh xạ chuẩn vào form
                            null, // Password không hiển thị lên form edit
                            rs.getString("FullName"),
                            rs.getString("Email"),
                            rs.getString("Phone"), // Ánh xạ chuẩn vào form
                            rs.getInt("RoleID"), // Ánh xạ chuẩn vào thẻ <select> chức vụ
                            rs.getString("RoleName"),
                            rs.getBoolean("IsActive") ? "Đang làm" : "Nghỉ việc",
                            "Chưa xếp ca",
                            rs.getInt("ShiftID")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // THÊM MỚI: HÀM CẬP NHẬT NHANH CA LÀM VIỆC XUỐNG DATABASE
    public boolean updateEmployeeShift(int userId, int shiftId) {
        String sql = "UPDATE Users SET ShiftID = ? WHERE UserID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (shiftId == 0) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, shiftId);
            }
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. THÊM MỚI NHÂN VIÊN CHI TIẾT
    public String addEmployeeDetailed(String username, String fullName, String email, String password, int roleId, String phone) {
        String checkAdminSql = "SELECT COUNT(*) FROM Users WHERE RoleID = 1";
        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        String checkEmailSql = "SELECT COUNT(*) FROM Users WHERE Email = ?";
        String checkPhoneSql = "SELECT COUNT(*) FROM Users WHERE Phone = ?";

        String sqlUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive) VALUES (?, ?, ?, ?, ?, ?, 1)";
        String sqlEmployee = "INSERT INTO Employees (EmployeeID, BranchID, SalaryRate, HireDate) VALUES (?, ?, ?, GETDATE())";

        Connection conn = null;
        try {
            conn = getConnection();
            if (roleId == 1) {
                try (PreparedStatement psCheck = conn.prepareStatement(checkAdminSql); ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "ADMIN_ALREADY_EXISTS";
                    }
                }
            }
            try (PreparedStatement psCheck = conn.prepareStatement(checkUserSql)) {
                psCheck.setString(1, username.trim());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "DUPLICATE_USERNAME";
                    }
                }
            }
            try (PreparedStatement psCheck = conn.prepareStatement(checkEmailSql)) {
                psCheck.setString(1, email.trim());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "DUPLICATE_EMAIL";
                    }
                }
            }
            try (PreparedStatement psCheck = conn.prepareStatement(checkPhoneSql)) {
                psCheck.setString(1, phone.trim());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "DUPLICATE_PHONE";
                    }
                }
            }

            conn.setAutoCommit(false);
            int newUserId = 0;
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, username.trim());
                psUser.setString(2, password);
                psUser.setString(3, fullName.trim());
                psUser.setString(4, email.trim());
                psUser.setString(5, phone.trim());
                psUser.setInt(6, roleId);
                psUser.executeUpdate();

                try (ResultSet rsKeys = psUser.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        newUserId = rsKeys.getInt(1);
                    }
                }
            }
            try (PreparedStatement psEmp = conn.prepareStatement(sqlEmployee)) {
                psEmp.setInt(1, newUserId);
                psEmp.setInt(2, 1);
                psEmp.setDouble(3, 30000.0);
                psEmp.executeUpdate();
            }

            conn.commit();
            return "SUCCESS";
        } catch (Exception e) {
            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) {
                        conn.rollback();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            System.out.println("❌ Lỗi DB: " + e.toString());
            return "SYSTEM_ERROR";
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }

    // 4. CẬP NHẬT NHÂN VIÊN
    public boolean updateEmployee(int id, String name, String email, int roleId, boolean isActive) {
        String sql = "UPDATE Users SET FullName = ?, Email = ?, RoleID = ?, IsActive = ? WHERE UserID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setInt(3, roleId);
            ps.setBoolean(4, isActive);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. ĐỔI TRẠNG THÁI KHÓA/MỞ KHÓA NHANH
    public boolean toggleStatus(int id) {
        String sql = "UPDATE Users SET IsActive = CASE WHEN IsActive = 1 THEN 0 ELSE 1 END WHERE UserID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. CẬP NHẬT ROLE NHANH
    public boolean updateEmployeeRole(int userId, int roleId) {
        String sql = "UPDATE Users SET RoleID = ? WHERE UserID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 7. LẤY DANH SÁCH ROLE
    public List<Role> getAllRoles() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT RoleID, RoleName FROM Roles WHERE RoleName != 'Customer'";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role r = new Role();
                r.setRoleId(rs.getInt("RoleID"));
                r.setRoleName(rs.getString("RoleName"));
                list.add(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 8. CẬP NHẬT NHÂN VIÊN CHI TIẾT
    public String updateEmployeeDetailed(int id, String username, String fullName, String email, String phone, int roleId, boolean isActive) {
        String checkAdminSql = "SELECT COUNT(*) FROM Users WHERE RoleID = 1 AND UserID != ?";
        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND UserID != ?";
        String checkEmailSql = "SELECT COUNT(*) FROM Users WHERE Email = ? AND UserID != ?";
        String checkPhoneSql = "SELECT COUNT(*) FROM Users WHERE Phone = ? AND UserID != ?";
        String updateSql = "UPDATE Users SET Username = ?, FullName = ?, Email = ?, Phone = ?, RoleID = ?, IsActive = ? WHERE UserID = ?";

        try (Connection conn = getConnection()) {
            if (roleId == 1) {
                try (PreparedStatement ps = conn.prepareStatement(checkAdminSql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            return "ADMIN_ALREADY_EXISTS";
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(checkUserSql)) {
                psCheck(ps, username, id, "DUPLICATE_USERNAME");
            }
            try (PreparedStatement ps = conn.prepareStatement(checkEmailSql)) {
                psCheck(ps, email, id, "DUPLICATE_EMAIL");
            }
            try (PreparedStatement ps = conn.prepareStatement(checkPhoneSql)) {
                psCheck(ps, phone, id, "DUPLICATE_PHONE");
            }
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, username.trim());
                ps.setString(2, fullName.trim());
                ps.setString(3, email.trim());
                ps.setString(4, phone.trim());
                ps.setInt(5, roleId);
                ps.setBoolean(6, isActive);
                ps.setInt(7, id);
                ps.executeUpdate();
            }
            return "SUCCESS";
        } catch (Exception e) {
            // Nhận diện lỗi ném ra từ hàm bổ trợ để trả về đúng trạng thái cho Servlet xử lý điều hướng
            if (e.getMessage() != null && e.getMessage().startsWith("DUPLICATE_")) {
                return e.getMessage();
            }
            System.out.println("❌ Lỗi Update DB: " + e.toString());
            return "SYSTEM_ERROR";
        }
    }

    private void psCheck(PreparedStatement ps, String field, int id, String errorCode) throws Exception {
        ps.setString(1, field.trim());
        ps.setInt(2, id);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                throw new Exception(errorCode);
            }
        }
    }

    // 9. HÀM BỔ TRỢ: ĐẾM SỐ LƯỢNG ADMIN KHÁC
    public boolean checkAdminExistsExcept(int currentUserId) {
        String sql = "SELECT COUNT(*) FROM Users WHERE RoleID = 1 AND UserID != ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    // 10. XÓA NHÂN VIÊN (Xóa bảng Employees trước, Users sau để tránh lỗi khóa ngoại)

    public boolean deleteEmployee(int userId) {
        String deleteEmpSql = "DELETE FROM Employees WHERE EmployeeID = ?";
        String deleteUserSql = "DELETE FROM Users WHERE UserID = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bật Transaction

            // Lệnh 1: Xóa trong bảng dữ liệu mở rộng Employees trước
            try (PreparedStatement psEmp = conn.prepareStatement(deleteEmpSql)) {
                psEmp.setInt(1, userId);
                psEmp.executeUpdate();
            }

            // Lệnh 2: Xóa tài khoản gốc trong bảng Users
            int rowsAffected = 0;
            try (PreparedStatement psUser = conn.prepareStatement(deleteUserSql)) {
                psUser.setInt(1, userId);
                rowsAffected = psUser.executeUpdate();
            }

            conn.commit(); // Thành công thì commit
            return rowsAffected > 0;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
