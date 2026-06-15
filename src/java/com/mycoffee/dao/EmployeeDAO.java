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

    // 1. LẤY TẤT CẢ NHÂN VIÊN (Trừ Khách hàng)
    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT u.UserID, u.FullName, u.Email, r.RoleName, u.IsActive "
                + "FROM Users u "
                + "JOIN Roles r ON u.RoleID = r.RoleID "
                + "WHERE r.RoleName != 'Customer'";

        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("UserID");
                String name = rs.getString("FullName");
                String email = rs.getString("Email");
                String role = rs.getString("RoleName");
                boolean isActive = rs.getBoolean("IsActive");

                String status = isActive ? "Đang làm" : "Nghỉ việc";
                String shift = "Chưa xếp ca";

                Employee emp = new Employee(id, name, email, role, status, shift);
                list.add(emp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. LẤY THÔNG TIN 1 NHÂN VIÊN THEO ID
    public Employee getEmployeeById(int id) {
        String sql = "SELECT u.UserID, u.FullName, u.Email, r.RoleName, u.IsActive "
                + "FROM Users u "
                + "JOIN Roles r ON u.RoleID = r.RoleID "
                + "WHERE u.UserID = ?";
        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Employee(
                            rs.getInt("UserID"),
                            rs.getString("FullName"),
                            rs.getString("Email"),
                            rs.getString("RoleName"),
                            rs.getBoolean("IsActive") ? "Đang làm" : "Nghỉ việc",
                            "Chưa xếp ca"
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. THÊM MỚI NHÂN VIÊN - TÁCH BIỆT HOÀN TOÀN USERNAME, EMAIL, PHONE VÀ FULLNAME
    public String addEmployeeDetailed(String username, String fullName, String email, String password, int roleId, String phone) {

        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        String checkEmailSql = "SELECT COUNT(*) FROM Users WHERE Email = ?";
        String checkPhoneSql = "SELECT COUNT(*) FROM Users WHERE Phone = ?";

        String sqlUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive) VALUES (?, ?, ?, ?, ?, ?, 1)";
        String sqlEmployee = "INSERT INTO Employees (EmployeeID, BranchID, SalaryRate, HireDate) VALUES (?, ?, ?, GETDATE())";

        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUser = null;
        PreparedStatement psEmp = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            // 1. Kiểm tra trùng Username ngắn gọn
            psCheck = conn.prepareStatement(checkUserSql);
            psCheck.setString(1, username.trim());
            rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_USERNAME";
            }
            rs.close();
            psCheck.close();

            // 2. Kiểm tra trùng Email
            psCheck = conn.prepareStatement(checkEmailSql);
            psCheck.setString(1, email.trim());
            rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_EMAIL";
            }
            rs.close();
            psCheck.close();

            // 3. Kiểm tra trùng Số điện thoại
            psCheck = conn.prepareStatement(checkPhoneSql);
            psCheck.setString(1, phone.trim());
            rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_PHONE";
            }
            rs.close();
            psCheck.close();

            // 4. Nếu tất cả đều duy nhất -> Tiến hành lưu vào database bọc trong Transaction
            conn.setAutoCommit(false);

            psUser = conn.prepareStatement(sqlUser, java.sql.Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, username.trim());
            psUser.setString(2, password);
            psUser.setString(3, fullName.trim()); 
            psUser.setString(4, email.trim());
            psUser.setString(5, phone.trim());
            psUser.setInt(6, roleId);

            psUser.executeUpdate();

            int newUserId = 0;
            rs = psUser.getGeneratedKeys();
            if (rs.next()) {
                newUserId = rs.getInt(1);
            }
            rs.close();

            // Thêm tiếp dữ liệu vào bảng liên kết chi tiết Employees
            psEmp = conn.prepareStatement(sqlEmployee);
            psEmp.setInt(1, newUserId);
            psEmp.setInt(2, 1);           // Chi nhánh mặc định: 1
            psEmp.setDouble(3, 30000.0);
            psEmp.executeUpdate();

            conn.commit();
            return "SUCCESS";

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception ex) {
                }
            }
            System.out.println("❌ Lỗi DB: " + e.toString());
            return "SYSTEM_ERROR";
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psCheck != null) psCheck.close(); } catch (Exception e) {}
            try { if (psUser != null) psUser.close(); } catch (Exception e) {}
            try { if (psEmp != null) psEmp.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    // 4. CẬP NHẬT NHÂN VIÊN (BẢN CŨ CỦA NHÓM - GIỮ LẠI ĐỂ TRÁNH LỖI PHẦN KHÁC)
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
        try (Connection conn = getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {

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

    // 8. CẬP NHẬT NHÂN VIÊN CHI TIẾT - ĐÃ ĐỒNG BỘ TÊN BIẾN VỚI CONTROLLER
    public String updateEmployeeDetailed(int id, String username, String fullName, String email, String phone, int roleId, boolean isActive) {
        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND UserID != ?";
        String checkEmailSql = "SELECT COUNT(*) FROM Users WHERE Email = ? AND UserID != ?";
        String checkPhoneSql = "SELECT COUNT(*) FROM Users WHERE Phone = ? AND UserID != ?";

        String updateSql = "UPDATE Users SET Username = ?, FullName = ?, Email = ?, Phone = ?, RoleID = ?, IsActive = ? WHERE UserID = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            // 1. Kiểm tra Username mới có trùng ai không (loại trừ chính id của mình)
            ps = conn.prepareStatement(checkUserSql);
            ps.setString(1, username.trim());
            ps.setInt(2, id);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_USERNAME";
            }
            rs.close();
            ps.close();

            // 2. Kiểm tra Email mới có trùng ai không
            ps = conn.prepareStatement(checkEmailSql);
            ps.setString(1, email.trim());
            ps.setInt(2, id);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_EMAIL";
            }
            rs.close();
            ps.close();

            // 3. Kiểm tra Phone mới có trùng ai không
            ps = conn.prepareStatement(checkPhoneSql);
            ps.setString(1, phone.trim());
            ps.setInt(2, id);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "DUPLICATE_PHONE";
            }
            rs.close();
            ps.close();

            // 4. Nếu không trùng ai -> Thực hiện cập nhật thông tin chính xác theo thứ tự
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, username.trim());
            ps.setString(2, fullName.trim());
            ps.setString(3, email.trim());
            ps.setString(4, phone.trim());
            ps.setInt(5, roleId);
            ps.setBoolean(6, isActive);
            ps.setInt(7, id);

            ps.executeUpdate();
            return "SUCCESS";

        } catch (Exception e) {
            System.out.println("❌ Lỗi Update DB: " + e.toString());
            return "SYSTEM_ERROR";
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}