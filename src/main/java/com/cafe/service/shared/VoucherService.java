package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.VoucherDao;
import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Shared voucher management and validation. */
public class VoucherService {

    private final VoucherDao dao = new VoucherDao();

    public List<Voucher> getVoucherList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public Voucher getVoucher(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createVoucher(Voucher v) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, v); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateVoucher(Voucher v) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, v); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void setVoucherActive(int id, boolean active) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateActive(conn, id, active); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void toggleActive(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Voucher v = dao.findById(conn, id);
                if (v != null) dao.updateActive(conn, id, !v.isActive());
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void incrementUsed(int voucherId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.incrementUsed(conn, voucherId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public String validateVoucher(String code, int branchId, BigDecimal orderAmount) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return validateVoucherRecord(dao.findByCode(conn, code), branchId, orderAmount);
        }
    }

    public String validateVoucherById(int voucherId, int branchId, BigDecimal orderAmount) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return validateVoucherRecord(dao.findById(conn, voucherId), branchId, orderAmount);
        }
    }

    public static String validateVoucherRecord(Voucher v, int branchId, BigDecimal orderAmount) {
        if (v == null || !v.isActive()) return "Voucher khong ton tai hoac da tat.";
        if ("BRANCH".equals(v.getScope()) && (v.getBranchId() == null || v.getBranchId() != branchId))
            return "Voucher khong ap dung cho chi nhanh nay.";
        LocalDateTime now = LocalDateTime.now();
        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) return "Voucher chua toi ngay ap dung.";
        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) return "Voucher da het han.";
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) return "Voucher da het luot su dung.";
        if (orderAmount != null && v.getMinOrderAmount() != null && orderAmount.compareTo(v.getMinOrderAmount()) < 0)
            return "Don chua dat gia tri toi thieu de dung voucher.";
        return null;
    }
}
