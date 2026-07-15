package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.VoucherDao;
import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A5 · VoucherService (đặc tả mục 3+4) — 1 NGUỒN duy nhất validate voucher.
 */
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

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
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

    /**
     * Validate voucher (1 nguồn duy nhất — Phase 5 Cashier gọi). Trả về thông điệp lỗi, null nếu hợp lệ.
     * (Tính toán số tiền giảm để Phase 5 dùng; ở Phase 2 chỉ cần đặt đúng tên & logic cơ bản.)
     */
    public String validateVoucher(String code, int branchId, BigDecimal orderAmount) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Voucher v = dao.findByCode(conn, code);
            if (v == null || !v.isActive()) return "Voucher không tồn tại hoặc đã tắt.";
            if ("BRANCH".equals(v.getScope()) && (v.getBranchId() == null || v.getBranchId() != branchId))
                return "Voucher không áp dụng cho chi nhánh này.";
            LocalDateTime now = LocalDateTime.now();
            if (v.getStartDate() != null && now.isBefore(v.getStartDate())) return "Voucher chưa tới ngày áp dụng.";
            if (v.getEndDate() != null && now.isAfter(v.getEndDate())) return "Voucher đã hết hạn.";
            if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) return "Voucher đã hết lượt sử dụng.";
            if (orderAmount != null && orderAmount.compareTo(v.getMinOrderAmount()) < 0)
                return "Đơn chưa đạt giá trị tối thiểu để dùng voucher.";
            return null; // hợp lệ
        }
    }
}
