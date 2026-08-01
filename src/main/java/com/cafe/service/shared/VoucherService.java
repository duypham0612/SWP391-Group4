package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.common.BusinessException;
import com.cafe.dao.shared.VoucherDao;
import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared voucher management and validation. */
public class VoucherService {

    private static final Set<String> TYPES = Set.of("PERCENT", "FIXED");
    private static final Set<String> SCOPES = Set.of("CHAIN", "BRANCH");
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]+");

    private final VoucherDao dao;

    public VoucherService() {
        this(new VoucherDao());
    }

    VoucherService(VoucherDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public List<Voucher> getVoucherList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public Voucher getVoucher(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public boolean isCodeInUse(String code, int excludeVoucherId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.existsByCode(conn, code, excludeVoucherId);
        }
    }

    public int createVoucher(Voucher v) throws SQLException {
        normalizeAndValidate(v);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (dao.existsByCode(conn, v.getCode(), 0)) throw new BusinessException("Mã voucher đã tồn tại.");
                int id = dao.insert(conn, v);
                conn.commit();
                return id;
            }
            catch (SQLException e) { conn.rollback(); throw translateWriteError(e); }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateVoucher(Voucher v) throws SQLException {
        normalizeAndValidate(v);
        if (v.getVoucherId() <= 0) throw new BusinessException("Mã voucher không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Voucher current = dao.findById(conn, v.getVoucherId());
                if (current == null) throw new BusinessException("Không tìm thấy voucher cần cập nhật.");
                v.setCode(current.getCode());
                if (dao.existsByCode(conn, v.getCode(), v.getVoucherId()))
                    throw new BusinessException("Mã voucher đã tồn tại.");
                dao.update(conn, v);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw translateWriteError(e); }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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
                if (v == null) throw new BusinessException("Không tìm thấy voucher.");
                if (!"RUNNING".equals(v.getLifecycleStatusCode()))
                    throw new BusinessException("Chỉ có thể bật hoặc tắt voucher đang diễn ra.");
                dao.updateActive(conn, id, !v.isActive());
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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
        return validateVoucherRecordAt(v, branchId, orderAmount, LocalDateTime.now(ZoneOffset.UTC));
    }

    /** Bản nhận mốc UTC tường minh để mọi boundary [start,end) kiểm thử được ổn định. */
    public static String validateVoucherRecordAt(Voucher v, int branchId, BigDecimal orderAmount,
                                                  LocalDateTime nowUtc) {
        if (v == null || !v.isActive()) return "Voucher không tồn tại hoặc đã tắt.";
        if ("BRANCH".equals(v.getScope()) && (v.getBranchId() == null || v.getBranchId() != branchId))
            return "Voucher không áp dụng cho chi nhánh này.";
        if (nowUtc == null) throw new IllegalArgumentException("Mốc UTC kiểm tra voucher là bắt buộc.");
        if (v.getStartAtUtc() != null && nowUtc.isBefore(v.getStartAtUtc())) return "Voucher chưa tới ngày áp dụng.";
        if (v.getEndAtUtc() != null && !nowUtc.isBefore(v.getEndAtUtc())) return "Voucher đã hết hạn.";
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) return "Voucher đã hết lượt sử dụng.";
        if (orderAmount != null && v.getMinOrderAmount() != null && orderAmount.compareTo(v.getMinOrderAmount()) < 0)
            return "Đơn chưa đạt giá trị tối thiểu để dùng voucher.";
        return null;
    }

    private static void normalizeAndValidate(Voucher voucher) {
        if (voucher == null) throw new BusinessException("Thông tin voucher là bắt buộc.");
        String code = voucher.getCode() == null ? null : voucher.getCode().trim().toUpperCase(Locale.ROOT);
        voucher.setCode(code);
        voucher.setDiscountType(upper(voucher.getDiscountType()));
        voucher.setScope(upper(voucher.getScope()));
        if (code == null || code.isBlank()) throw new BusinessException("Mã voucher không được để trống.");
        if (code.length() > 40) throw new BusinessException("Mã voucher không được vượt quá 40 ký tự.");
        if (!CODE_PATTERN.matcher(code).matches())
            throw new BusinessException("Mã voucher chỉ được chứa chữ cái không dấu, chữ số, dấu gạch ngang hoặc gạch dưới.");
        if (!TYPES.contains(voucher.getDiscountType()))
            throw new BusinessException("Loại giảm phải là PERCENT hoặc FIXED.");
        if (voucher.getDiscountValue() == null || voucher.getDiscountValue().signum() < 0)
            throw new BusinessException("Giá trị giảm phải lớn hơn hoặc bằng 0.");
        if ("PERCENT".equals(voucher.getDiscountType())
                && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new BusinessException("Giảm theo phần trăm không được vượt quá 100.");
        if (!SCOPES.contains(voucher.getScope())) throw new BusinessException("Phạm vi áp dụng không hợp lệ.");
        if ("BRANCH".equals(voucher.getScope()) && (voucher.getBranchId() == null || voucher.getBranchId() <= 0))
            throw new BusinessException("Vui lòng chọn chi nhánh áp dụng.");
        if ("CHAIN".equals(voucher.getScope())) voucher.setBranchId(null);
        if (voucher.getMinOrderAmount() == null || voucher.getMinOrderAmount().signum() < 0)
            throw new BusinessException("Giá trị đơn tối thiểu phải lớn hơn hoặc bằng 0.");
        if (voucher.getUsageLimit() != null && voucher.getUsageLimit() < 0)
            throw new BusinessException("Giới hạn sử dụng phải lớn hơn hoặc bằng 0.");
        if (voucher.getStartAtUtc() != null && voucher.getEndAtUtc() != null
                && !voucher.getStartAtUtc().isBefore(voucher.getEndAtUtc())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static SQLException translateWriteError(SQLException error) {
        if (error.getErrorCode() == 2601 || error.getErrorCode() == 2627)
            throw new BusinessException("Mã voucher đã tồn tại.");
        return error;
    }
}
