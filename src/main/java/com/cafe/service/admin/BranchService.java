package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.org.BranchDao;
import com.cafe.model.Branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * A2 · BranchService (đặc tả mục 4).
 */
public class BranchService {

    private final BranchDao dao;

    public BranchService() {
        this(new BranchDao());
    }

    BranchService(BranchDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

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
        normalizeAndValidate(b);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = dao.insert(conn, b);
                if (id <= 0) throw new SQLException("Không lấy được BranchId sau khi tạo chi nhánh.");
                dao.updateCode(conn, id, String.format("CN%02d", id));
                conn.commit();
                return id;
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateBranch(Branch b) throws SQLException {
        normalizeAndValidate(b);
        if (b.getBranchId() <= 0) throw new BusinessException("Mã chi nhánh không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Branch current = dao.findById(conn, b.getBranchId());
                if (current == null) throw new BusinessException("Không tìm thấy chi nhánh cần cập nhật.");
                b.setCode(current.getCode());
                b.setManagerUserId(current.getManagerUserId());
                b.setManagerName(current.getManagerName());
                dao.update(conn, b);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Manager tự cài giờ mở/đóng cửa + ngưỡng cao điểm cho chi nhánh mình (không đụng cột khác). */
    public void updateHoursAndPeak(int branchId, java.time.LocalTime openTime,
                                   java.time.LocalTime closeTime, int peakThresholdCups) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateHoursAndPeak(conn, branchId, openTime, closeTime, peakThresholdCups); conn.commit(); }
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
                if (b == null) throw new BusinessException("Không tìm thấy chi nhánh.");
                if (b.getManagerUserId() == null)
                    throw new BusinessException("Vui lòng phân công quản lý trước khi thay đổi trạng thái chi nhánh.");
                dao.updateActive(conn, id, !b.isActive());
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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

    private static void normalizeAndValidate(Branch branch) {
        if (branch == null) throw new BusinessException("Thông tin chi nhánh là bắt buộc.");
        String name = clean(branch.getName());
        String address = clean(branch.getAddress());
        if (name == null) throw new BusinessException("Tên chi nhánh không được để trống.");
        if (address == null) throw new BusinessException("Địa chỉ không được để trống.");
        if ((branch.getOpenTime() == null) != (branch.getCloseTime() == null))
            throw new BusinessException("Giờ mở cửa và giờ đóng cửa phải nhập cả hai hoặc để trống cả hai.");
        if (branch.getOpenTime() != null && !branch.getOpenTime().isBefore(branch.getCloseTime()))
            throw new BusinessException("Giờ mở cửa phải trước giờ đóng cửa trong cùng ngày.");
        branch.setName(name);
        branch.setAddress(address);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
