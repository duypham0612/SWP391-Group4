package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    // Kiểm tra xem Username hoặc Email đã tồn tại hay chưa
    public boolean isUsernameOrEmailExists(String username, String email) {
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ? OR Email = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham isUsernameOrEmailExists trong UserDAO: " + e.getMessage());
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
}
