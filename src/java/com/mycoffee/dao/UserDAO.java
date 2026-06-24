package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class UserDAO {

    // Hàm kiểm tra thông tin Đăng nhập (Username hoặc Email)
    public User checkLogin(String usernameOrEmail, String password) {
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt "
                   + "FROM Users "
                   + "WHERE (Username = ? OR Email = ?) AND Password = ? AND IsActive = 1";

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


    /**
     * Đăng ký tài khoản mới cho Khách hàng.
     * BẢO MẬT: RoleID được SET CỨNG = 4 (Customer) tại đây.
     * Tuyệt đối KHÔNG nhận RoleID từ phía client truyền lên.
     * Sau khi insert Users thành công, tự động insert bản ghi vào bảng Customers
     * để kích hoạt tính năng tích điểm. Toàn bộ thực hiện trong 1 TRANSACTION.
     */
    public boolean registerUser(User user) {
        // ĐẶT CỨNG RoleID = 4 (Customer) — không lấy từ client
        final int CUSTOMER_ROLE_ID = 4;

        String sqlInsertUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) "
                             + "VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        String sqlInsertCustomer = "INSERT INTO Customers (CustomerID, MemberRank, CurrentPoints) VALUES (?, N'Member', 0)";

        Connection conn = null;
        try {
            conn = new DBContext().getConnection();
            conn.setAutoCommit(false); // Bắt đầu TRANSACTION

            // Bước 1: Insert vào bảng Users, lấy lại UserID vừa được sinh ra
            int newUserId = -1;
            try (PreparedStatement psUser = conn.prepareStatement(sqlInsertUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, user.getUsername());
                psUser.setString(2, user.getPassword());
                psUser.setString(3, user.getFullName());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, user.getPhone());
                psUser.setInt(6, CUSTOMER_ROLE_ID); // SET CỨNG tại đây
                int rows = psUser.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
                try (ResultSet generatedKeys = psUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newUserId = generatedKeys.getInt(1);
                    }
                }
            }

            if (newUserId == -1) {
                conn.rollback();
                return false;
            }

            // Bước 2: Insert vào bảng Customers để kích hoạt tích điểm
            try (PreparedStatement psCustomer = conn.prepareStatement(sqlInsertCustomer)) {
                psCustomer.setInt(1, newUserId);
                psCustomer.executeUpdate();
            }

            conn.commit(); // COMMIT nếu cả 2 bước đều thành công
            return true;

        } catch (Exception e) {
            System.out.println("Loi ham registerUser trong UserDAO: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback(); // ROLLBACK nếu có lỗi
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
