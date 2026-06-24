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
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham checkLogin trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean isUserExists(String username, String email, String phone) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Users WHERE Username = ?");
        List<String> params = new ArrayList<>();
        params.add(username);

        if (email != null) {
            sql.append(" OR Email = ?");
            params.add(email);
        }
        if (phone != null) {
            sql.append(" OR Phone = ?");
            params.add(phone);
        }

        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
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
        final int CUSTOMER_ROLE_ID = User.ROLE_CUSTOMER;

        String sqlInsertUser = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) "
                + "OUTPUT INSERTED.UserID "
                + "VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        String sqlInsertCustomer = "INSERT INTO Customers (CustomerID, MemberRank, CurrentPoints) VALUES (?, N'Member', 0)";

        Connection conn = null;
        try {
            conn = new DBContext().getConnection();
            conn.setAutoCommit(false); // Bắt đầu TRANSACTION

            // Bước 1: Insert vào bảng Users, lấy lại UserID vừa được sinh ra
            int newUserId = -1;
            try (PreparedStatement psUser = conn.prepareStatement(sqlInsertUser)) {
                psUser.setString(1, user.getUsername());
                psUser.setString(2, user.getPassword());
                psUser.setString(3, user.getFullName());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, user.getPhone());
                psUser.setInt(6, CUSTOMER_ROLE_ID); // SET CỨNG tại đây
                try (ResultSet insertedUser = psUser.executeQuery()) {
                    if (insertedUser.next()) {
                        newUserId = insertedUser.getInt("UserID");
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
                if (conn != null)
                    conn.rollback(); // ROLLBACK nếu có lỗi
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public User getUserById(int userId) {
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt "
                + "FROM Users WHERE UserID = ?";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham getUserById trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT UserID, Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt "
                + "FROM Users ORDER BY RoleID, UserID";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (Exception e) {
            System.out.println("Loi ham getAllUsers trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO Users (Username, Password, FullName, Email, Phone, RoleID, IsActive, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE())";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setInt(6, user.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham addUser trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateUser(User user) {
        String sql;
        boolean updatePassword = user.getPassword() != null && !user.getPassword().trim().isEmpty();
        if (updatePassword) {
            sql = "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, RoleID = ?, Password = ? WHERE UserID = ?";
        } else {
            sql = "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, RoleID = ? WHERE UserID = ?";
        }

        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setInt(4, user.getRoleId());
            if (updatePassword) {
                ps.setString(5, user.getPassword());
                ps.setInt(6, user.getUserId());
            } else {
                ps.setInt(5, user.getUserId());
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham updateUser trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        String deleteEmployee = "DELETE FROM Employees WHERE EmployeeID = ?";
        String deleteCustomer = "DELETE FROM Customers WHERE CustomerID = ?";
        String deleteUser = "DELETE FROM Users WHERE UserID = ?";

        Connection conn = null;
        try {
            conn = new DBContext().getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(deleteEmployee)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteCustomer)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            int rows;
            try (PreparedStatement ps = conn.prepareStatement(deleteUser)) {
                ps.setInt(1, userId);
                rows = ps.executeUpdate();
            }
            conn.commit();
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Loi ham deleteUser trong UserDAO: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String checkDuplicateFields(String username, String email, String phone) {
        String sql = "SELECT Username, Email, Phone FROM Users WHERE Username = ? OR Email = ? OR Phone = ?";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (username != null && username.equalsIgnoreCase(rs.getString("Username"))) {
                        return "Tên đăng nhập đã tồn tại.";
                    }
                    if (email != null && email.equalsIgnoreCase(rs.getString("Email"))) {
                        return "Email đã tồn tại.";
                    }
                    if (phone != null && phone.equals(rs.getString("Phone"))) {
                        return "Số điện thoại đã tồn tại.";
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham checkDuplicateFields trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String checkDuplicateFieldsForUpdate(int userId, String email, String phone) {
        String sql = "SELECT Email, Phone FROM Users WHERE UserID <> ? AND (Email = ? OR Phone = ?)";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, email);
            ps.setString(3, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (email != null && email.equalsIgnoreCase(rs.getString("Email"))) {
                        return "Email đã tồn tại.";
                    }
                    if (phone != null && phone.equals(rs.getString("Phone"))) {
                        return "Số điện thoại đã tồn tại.";
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Loi ham checkDuplicateFieldsForUpdate trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean toggleUserStatus(int userId, boolean status) {
        String sql = "UPDATE Users SET IsActive = ? WHERE UserID = ?";
        try (Connection conn = new DBContext().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi ham toggleUserStatus trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws Exception {
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
