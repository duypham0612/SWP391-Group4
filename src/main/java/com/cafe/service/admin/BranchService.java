package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.BranchDao;
import com.cafe.model.Branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A2 · BranchService (đặc tả mục 4).
 */
public class BranchService {

    private final BranchDao dao = new BranchDao();

    public List<Branch> getBranchList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<Branch> getBranchListActive() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAllActive(conn); }
    }

    public Branch getBranch(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createBranch(Branch b) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, b); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateBranch(Branch b) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, b); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void setBranchActive(int id, boolean active) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateActive(conn, id, active); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Branch b = dao.findById(conn, id);
                if (b != null) dao.updateActive(conn, id, !b.isActive());
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void assignManager(int branchId, Integer userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateManager(conn, branchId, userId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
