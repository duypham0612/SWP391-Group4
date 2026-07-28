package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.common.PasswordHasher;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.admin.UserDao;
import com.cafe.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A1 · UserService (đặc tả mục 4) — quản lý nhân sự (Admin).
 * Mật khẩu mới được băm BCrypt tại Service.
 */
public class UserService {

    private final UserDao dao = new UserDao();
    private final BranchDao branchDao = new BranchDao();

    public List<User> getUserList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<User> getUserListByBranch(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByBranch(conn, branchId); }
    }

    /** A2 · danh sách nhân sự có lọc theo vai trò/chi nhánh/từ khoá (null = bỏ qua). */
    public List<User> getUserList(Integer roleId, Integer branchId, String q, int offset, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.findFiltered(conn, roleId, branchId, q, offset, limit);
        }
    }

    public int countUsers(Integer roleId, Integer branchId, String q) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.countFiltered(conn, roleId, branchId, q);
        }
    }

    /** A2.F6 · danh sách quản lý chi nhánh (cho dropdown gán Manager). */
    public List<User> getManagers() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByRoleCode(conn, "BRANCH_MANAGER"); }
    }

    public User getUser(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public boolean usernameTaken(String username, int excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.usernameExists(conn, username, excludeId);
        }
    }

    public boolean isBranchManagerRole(int roleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.hasRoleCode(conn, roleId, "BRANCH_MANAGER");
        }
    }

    public boolean branchHasOtherManager(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Integer managerId = branchDao.findManagerUserIdForUpdate(conn, branchId);
            return managerId != null && managerId != userId;
        }
    }

    public boolean isAssignedBranchManager(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return branchDao.isManagerAssigned(conn, userId);
        }
    }

    public int createUser(User u, String rawPassword) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean manager = dao.hasRoleCode(conn, u.getRoleId(), "BRANCH_MANAGER");
                ensureManagerSlot(conn, manager, u.getBranchId(), 0);
                int id = dao.insert(conn, u, PasswordHasher.hashPassword(rawPassword));
                if (manager) branchDao.updateManager(conn, u.getBranchId(), id);
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateUser(User u) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean manager = dao.hasRoleCode(conn, u.getRoleId(), "BRANCH_MANAGER");
                ensureManagerSlot(conn, manager, u.getBranchId(), u.getUserId());
                dao.update(conn, u);
                branchDao.clearManagerByUser(conn, u.getUserId());
                if (manager) branchDao.updateManager(conn, u.getBranchId(), u.getUserId());
                conn.commit();
            }
            catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void ensureManagerSlot(Connection conn, boolean manager, Integer branchId, int userId)
            throws SQLException {
        if (!manager) return;
        if (branchId == null) throw new BusinessException("Quản lý phải thuộc một chi nhánh.");
        Integer currentManagerId = branchDao.findManagerUserIdForUpdate(conn, branchId);
        if (currentManagerId != null && currentManagerId != userId) {
            throw new BusinessException("Chi nhánh đã có quản lý phụ trách.");
        }
    }

    public void updateProfile(int userId, String fullName, String email, String phone) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateProfile(conn, userId, fullName, email, phone); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void setUserStatus(int userId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateStatus(conn, userId, status); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void resetPassword(int userId, String rawPassword) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updatePassword(conn, userId, PasswordHasher.hashPassword(rawPassword)); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void assignBranch(int userId, Integer branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User u = dao.findById(conn, userId);
                if (u != null) {
                    u.setBranchId(branchId);
                    dao.update(conn, u);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
