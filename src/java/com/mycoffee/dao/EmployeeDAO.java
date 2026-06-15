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

    // 3. THÊM MỚI NHÂN VIÊN - ĐÃ TỐI ƯU DEBUG
    public boolean addEmployee(String name, String email, String password, int roleId) {
        String sql = "INSERT INTO Users (FullName, Email, Password, RoleID, IsActive) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
           
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setInt(4, roleId);
            ps.setInt(5, 1); // IsActive = true

            int rows = ps.executeUpdate();
            System.out.println("✅ [DAO] INSERT thành công | Rows affected: " + rows 
                    + " | Email: " + email + " | RoleID: " + roleId);
            return rows > 0;

        } catch (Exception e) {
            System.out.println("❌ [DAO] LỖI KHI THÊM NHÂN VIÊN:");
            System.out.println("   Email: " + email);
            System.out.println("   RoleID: " + roleId);
            System.out.println("   Nguyên nhân: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 4. CẬP NHẬT NHÂN VIÊN
    public boolean updateEmployee(int id, String name, String email, int roleId, boolean isActive) {
        String sql = "UPDATE Users SET FullName = ?, Email = ?, RoleID = ?, IsActive = ? WHERE UserID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
           
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

    // 5. ĐỔI TRẠNG THÁI
    public boolean toggleStatus(int id) {
        String sql = "UPDATE Users SET IsActive = CASE WHEN IsActive = 1 THEN 0 ELSE 1 END WHERE UserID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
           
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
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
           
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
}