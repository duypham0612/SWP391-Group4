package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.model.BranchMenuItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BranchMenuService {

    private final BranchMenuDao dao = new BranchMenuDao();

    public List<BranchMenuItem> listForBranch(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.listForBranch(conn, branchId); }
    }

    public void save(int branchId, int productId, boolean available, BigDecimal localPrice, boolean is86) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.upsert(conn, branchId, productId, available, localPrice, is86); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** B3 · 86 board (Barista) — bật/tắt hết món; khoá món khỏi POS + QR menu. */
    public void set86(int branchId, int productId, boolean is86) throws SQLException {
        set86(branchId, productId, is86, null);
    }

    /** B3.F3 · 86 kèm ETA dự kiến có lại (NULL = chưa rõ); mở bán lại tự xoá ETA. */
    public void set86(int branchId, int productId, boolean is86, java.time.LocalDateTime backInEta) throws SQLException {
        java.sql.Timestamp ts = backInEta == null ? null : java.sql.Timestamp.valueOf(backInEta);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateIs86(conn, branchId, productId, is86, ts); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Danh sách món của chi nhánh (cho 86 board). */
    public List<BranchMenuItem> getMenuAvailability(int branchId) throws SQLException {
        return listForBranch(branchId);
    }

    public void remove(int branchId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.remove(conn, branchId, productId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
