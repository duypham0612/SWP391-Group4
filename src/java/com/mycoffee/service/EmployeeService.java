package com.mycoffee.service;

import com.mycoffee.dao.UserDAO;
import com.mycoffee.model.User;
import java.util.List;

public class EmployeeService {
    private final UserDAO userDAO = new UserDAO();

    // Lấy thông tin chi tiết một tài khoản bằng ID (Dùng để kiểm tra phân quyền bảo mật)
    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    // Lấy danh sách toàn bộ tài khoản
    public List<User> getAllEmployees() {
        return userDAO.getAllUsers();
    }

    // Thêm nhân viên mới với mật khẩu mặc định tự sinh là "123456"
    public boolean addEmployee(User user) {
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword("123456"); 
        }
        return userDAO.addUser(user);
    }

    // Cập nhật thông tin nhân viên
    public boolean updateEmployee(User user) {
        return userDAO.updateUser(user);
    }

    // Xóa tài khoản nhân viên ra khỏi hệ thống
    public boolean deleteEmployee(int userId) {
        return userDAO.deleteUser(userId);
    }

    // Kiểm tra trùng lặp trước khi THÊM MỚI (Username, Email, Phone)
    public String checkDuplicate(String username, String email, String phone) {
        return userDAO.checkDuplicateFields(username, email, phone);
    }

    // Kiểm tra trùng lặp trước khi CẬP NHẬT (Loại trừ ID hiện tại để sửa chính mình không báo trùng)
    public String checkDuplicateForUpdate(int userId, String email, String phone) {
        return userDAO.checkDuplicateFieldsForUpdate(userId, email, phone);
    }

    // --- BỔ SUNG: Thay đổi trạng thái hoạt động khóa/mở tài khoản nhân viên ---
    public boolean toggleEmployeeStatus(int userId, boolean status) {
        return userDAO.toggleUserStatus(userId, status);
    }
}
