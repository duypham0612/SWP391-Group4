package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Hàm lấy thông tin một User cụ thể bằng ID
    public User getUserById(int userId) {
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt FROM Users WHERE UserID = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
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
            e.printStackTrace();
        }
        return null;
    }

    // Hàm kiểm tra thông tin Đăng nhập (Username hoặc Email)
    public User checkLogin(String usernameOrEmail, String password) {
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt "
                + "FROM Users "
                + "WHERE (Username = ? OR Email = ?) AND Password = ? AND IsActive = 1";

        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // Kiểm tra độc lập dữ liệu trùng lặp khi tạo mới + Định dạng Phone
    public String checkDuplicateFields(String username, String email, String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.trim().matches("^0\\d{9}$")) {
                return "Số điện thoại không hợp lệ! Phải bao gồm đúng 10 chữ số và bắt đầu bằng số 0.";
            }
        }

        // 1. Kiểm tra Username
        String sqlUser = "SELECT COUNT(*) FROM Users WHERE Username = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sqlUser)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return "Tên tài khoản đã tồn tại!";
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Kiểm tra Email
        String sqlEmail = "SELECT COUNT(*) FROM Users WHERE Email = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sqlEmail)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return "Địa chỉ email đã tồn tại!";
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 3. Kiểm tra Phone
        if (phone != null && !phone.trim().isEmpty()) {
            String sqlPhone = "SELECT COUNT(*) FROM Users WHERE Phone = ?";
            try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sqlPhone)) {
                ps.setString(1, phone.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return "Số điện thoại đã tồn tại!";
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null; 
    }

    // Kiểm tra trùng lặp dành riêng cho tác vụ CẬP NHẬT
    public String checkDuplicateFieldsForUpdate(int userId, String email, String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.trim().matches("^0\\d{9}$")) {
                return "Số điện thoại không hợp lệ! Phải bao gồm đúng 10 chữ số và bắt đầu bằng số 0.";
            }
        }

        // 1. Kiểm tra Email trùng
        String sqlEmail = "SELECT COUNT(*) FROM Users WHERE Email = ? AND UserID != ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sqlEmail)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return "Địa chỉ email đã được sử dụng bởi tài khoản khác!";
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Kiểm tra Số điện thoại trùng
        if (phone != null && !phone.trim().isEmpty()) {
            String sqlPhone = "SELECT COUNT(*) FROM Users WHERE Phone = ? AND UserID != ?";
            try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sqlPhone)) {
                ps.setString(1, phone.trim());
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return "Số điện thoại đã được sử dụng bởi tài khoản khác!";
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return null;
    }

    public boolean hasAdmin() {
        String sql = "SELECT COUNT(*) FROM Users WHERE RoleID = 1";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    /**
     * Đăng ký tài khoản mới cho Khách hàng.
     * BẢO MẬT: RoleID được SET CỨNG = 4 (Customer) tại đây.
     * Sau khi insert Users thành công, tự động insert bản ghi vào bảng Customers
     * để kích hoạt tính năng tích điểm. Toàn bộ thực hiện trong 1 TRANSACTION.
     */
    public boolean registerUser(User user) {
        final int CUSTOMER_ROLE_ID = 4;
        String sqlInsertUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        String sqlInsertCustomer = "INSERT INTO Customers (CustomerID, MemberRank, CurrentPoints) VALUES (?, N'Member', 0)";

        Connection conn = null;
        try {
            conn = new DBContext().getConnection();
            conn.setAutoCommit(false); // Bắt đầu TRANSACTION

            int newUserId = -1;
            try (PreparedStatement psUser = conn.prepareStatement(sqlInsertUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, user.getUsername());
                psUser.setString(2, user.getPassword());
                psUser.setString(3, user.getFullName());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, user.getPhone() != null ? user.getPhone().trim() : null);
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
                if (conn != null) conn.rollback(); 
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

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt FROM Users ORDER BY CreatedAt DESC";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
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
                list.add(user);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public int getTotalUsersCount() {
        String sql = "SELECT COUNT(*) FROM Users";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public List<User> getUsersWithPagination(int page, int recordsPerPage) {
        List<User> list = new ArrayList<>();
        int offset = (page - 1) * recordsPerPage;
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt FROM Users ORDER BY CreatedAt DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, recordsPerPage);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    list.add(user);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public List<User> searchUsersRealtime(String keyword, String filter) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt FROM Users WHERE 1=1 ";
        if (keyword != null && !keyword.trim().isEmpty()) {
            keyword = "%" + keyword.trim() + "%";
            switch (filter) {
                case "id": sql += " AND CAST(UserID AS VARCHAR) LIKE ?"; break;
                case "name": sql += " AND FullName LIKE ?"; break;
                case "email": sql += " AND Email LIKE ?"; break;
                case "phone": sql += " AND Phone LIKE ?"; break;
                case "role":
                    String rawKey = keyword.replace("%", "").toLowerCase();
                    if ("admin".contains(rawKey)) sql += " AND RoleID = 1";
                    else if ("manager".contains(rawKey)) sql += " AND RoleID = 2";
                    else if ("staff".contains(rawKey)) sql += " AND RoleID = 3";
                    else if ("customer".contains(rawKey)) sql += " AND RoleID = 4";
                    break;
                default: sql += " AND (Username LIKE ? OR FullName LIKE ? OR Email LIKE ? OR Phone LIKE ? OR CAST(UserID AS VARCHAR) LIKE ?)"; break;
            }
        }
        sql += " ORDER BY CreatedAt DESC";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                if ("all".equals(filter) || filter == null || filter.trim().isEmpty()) {
                    ps.setString(1, keyword); ps.setString(2, keyword); ps.setString(3, keyword); ps.setString(4, keyword); ps.setString(5, keyword.replace("#USR", "").replace("#usr", ""));
                } else if ("id".equals(filter)) {
                    ps.setString(1, keyword.replace("#USR", "").replace("#usr", ""));
                } else if (!"role".equals(filter)) {
                    ps.setString(1, keyword);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setUsername(rs.getString("Username"));
                    user.setFullName(rs.getString("FullName"));
                    user.setEmail(rs.getString("Email"));
                    user.setPhone(rs.getString("Phone"));
                    user.setRoleId(rs.getInt("RoleID"));
                    user.setIsActive(rs.getBoolean("IsActive"));
                    user.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    list.add(user);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone() != null ? user.getPhone().trim() : null);
            ps.setInt(6, user.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { System.out.println("Loi ham addUser: " + e.getMessage()); }
        return false;
    }

    public boolean updateUser(User user) {
        boolean hasNewPassword = (user.getPassword() != null && !user.getPassword().trim().isEmpty());
        String sql = hasNewPassword 
            ? "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, RoleID = ?, Password = ? WHERE UserID = ?"
            : "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, RoleID = ? WHERE UserID = ?";

        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone() != null ? user.getPhone().trim() : null);
            ps.setInt(4, user.getRoleId());

            if (hasNewPassword) {
                ps.setString(5, user.getPassword().trim()); 
                ps.setInt(6, user.getUserId());
            } else {
                ps.setInt(5, user.getUserId());
            }

            return ps.executeUpdate() > 0;
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE UserID = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean isUserExists(String username, String email, String phone) {
        return this.checkDuplicateFields(username, email, phone) != null;
    }

    // --- Hàm thay đổi trạng thái hoạt động khóa/mở tài khoản ---
    public boolean toggleUserStatus(int userId, boolean status) {
        String sql = "UPDATE Users SET IsActive = ? WHERE UserID = ?";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}