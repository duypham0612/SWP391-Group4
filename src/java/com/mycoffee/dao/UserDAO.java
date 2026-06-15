package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Hàm kiểm tra thông tin Đăng nhập (Username hoặc Email)
    public User checkLogin(String usernameOrEmail, String password) {
        // ĐÃ SỬA: Xóa điều kiện "AND IsActive = 1" để lấy được tài khoản bị khóa lên check
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt "
                   + "FROM Users "
                   + "WHERE (Username = ? OR Email = ?) AND Password = ?";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ps.setString(3, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setUsername(rs.getString("Username"));
                    user.setPassword(rs.getString("Password"));
                    user.setFullName(rs.getString("FullName"));
                    user.setEmail(rs.getString("Email"));
                    user.setPhone(rs.getString("Phone"));
                    user.setRoleId(rs.getInt("RoleID"));
                    user.setIsActive(rs.getBoolean("IsActive"));
                    user.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    return user;
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham checkLogin trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Kiểm tra xem Username, Email hoặc Phone đã tồn tại hay chưa
    public boolean isUserExists(String username, String email, String phone) {
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ? OR Email = ? OR Phone = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham isUserExists trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Đăng ký tài khoản mới cho Khách hàng (mặc định RoleID = 2, IsActive = 1)
    public boolean registerUser(User user) {
        String sql = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) "
                   + "VALUES (?, ?, ?, ?, ?, 2, 1, CURRENT_TIMESTAMP)";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Loi ham registerUser trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================================
    // THÀNH PHẦN THÊM MỚI: CÁC HÀM CRUD QUẢN LÝ NHÂN VIÊN DÀNH CHO ADMIN
    // =========================================================================

    // 1. READ: Lấy danh sách tất cả nhân viên (Kết hợp thông tin bảng Users và Employees)
    public List<User> getAllEmployees() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.UserID, u.Username, u.FullName, u.Email, u.Phone, u.IsActive, u.RoleID, "
                   + "       e.BranchID, b.BranchName, e.SalaryRate, r.RoleName "
                   + "FROM Users u "
                   + "JOIN Employees e ON u.UserID = e.EmployeeID "
                   + "JOIN Branches b ON e.BranchID = b.BranchID "
                   + "JOIN Roles r ON u.RoleID = r.RoleID";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("UserID"));
                u.setUsername(rs.getString("Username"));
                u.setFullName(rs.getString("FullName"));
                u.setEmail(rs.getString("Email"));
                u.setPhone(rs.getString("Phone"));
                u.setIsActive(rs.getBoolean("IsActive"));
                u.setRoleId(rs.getInt("RoleID"));
                u.setBranchId(rs.getInt("BranchID"));
                u.setBranchName(rs.getString("BranchName"));
                u.setSalaryRate(rs.getDouble("SalaryRate"));
                u.setRoleName(rs.getString("RoleName"));
                list.add(u);
            }
        } catch (Exception e) {
            System.out.println("Loi ham getAllEmployees trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 2. CREATE: Thêm đồng thời vào Users và Employees (Sử dụng Transaction an toàn dữ liệu)
    public boolean insertEmployee(User user) {
        String sqlUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        String sqlEmp = "INSERT INTO Employees (EmployeeID, BranchID, SalaryRate, HireDate) VALUES (?, ?, ?, GETDATE())";
        
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false); // Bật chế độ quản lý giao dịch Transaction
            
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, java.sql.Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psEmp = conn.prepareStatement(sqlEmp)) {
                
                // Thêm vào bảng Users trước
                psUser.setString(1, user.getUsername());
                psUser.setString(2, user.getPassword());
                psUser.setString(3, user.getFullName());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, user.getPhone());
                psUser.setInt(6, user.getRoleId());
                
                int affectedRows = psUser.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }
                
                // Lấy UserID tự động sinh từ database
                int generatedUserId = 0;
                try (ResultSet rs = psUser.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1);
                    }
                }
                
                if (generatedUserId == 0) {
                    conn.rollback();
                    return false;
                }
                
                // Thêm tiếp vào bảng Employees sử dụng mã định danh vừa sinh ra
                psEmp.setInt(1, generatedUserId);
                psEmp.setInt(2, user.getBranchId());
                psEmp.setDouble(3, user.getSalaryRate());
                psEmp.executeUpdate();
                
                conn.commit(); // Hoàn thành xuất sắc toàn bộ tiến trình -> Lưu dữ liệu
                return true;
            } catch (Exception e) {
                conn.rollback(); // Hủy bỏ, hoàn tác thao tác nếu có bất kỳ lỗi nào xuất hiện giữa chừng
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Loi ham insertEmployee trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 3. UPDATE: Cập nhật thông tin sửa đổi nhân viên trên cả 2 bảng cùng lúc
    public boolean updateEmployee(User user) {
        String sqlUser = "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, RoleID = ?, IsActive = ? WHERE UserID = ?";
        String sqlEmp = "UPDATE Employees SET BranchID = ?, SalaryRate = ? WHERE EmployeeID = ?";
        
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser);
                 PreparedStatement psEmp = conn.prepareStatement(sqlEmp)) {
                
                // Chỉnh sửa thông tin bảng Users
                psUser.setString(1, user.getFullName());
                psUser.setString(2, user.getEmail());
                psUser.setString(3, user.getPhone());
                psUser.setInt(4, user.getRoleId());
                psUser.setBoolean(5, user.isIsActive());
                psUser.setInt(6, user.getUserId());
                psUser.executeUpdate();
                
                // Chỉnh sửa thông tin bảng Employees
                psEmp.setInt(1, user.getBranchId());
                psEmp.setDouble(2, user.getSalaryRate());
                psEmp.setInt(3, user.getUserId());
                psEmp.executeUpdate();
                
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Loi ham updateEmployee trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 4. DELETE: Xóa dữ liệu nhân sự (Xóa bảng con Employees trước rồi mới xóa Users sau)
    public boolean deleteEmployee(int userId) {
        String sqlEmp = "DELETE FROM Employees WHERE EmployeeID = ?";
        String sqlUser = "DELETE FROM Users WHERE UserID = ?";
        
        try (Connection conn = new DBContext().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psEmp = conn.prepareStatement(sqlEmp);
                 PreparedStatement psUser = conn.prepareStatement(sqlUser)) {
                
                // Thực hiện xóa ở bảng phụ thuộc trước
                psEmp.setInt(1, userId);
                psEmp.executeUpdate();
                
                // Thực hiện xóa tài khoản gốc
                psUser.setInt(1, userId);
                psUser.executeUpdate();
                
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Loi ham deleteEmployee trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}