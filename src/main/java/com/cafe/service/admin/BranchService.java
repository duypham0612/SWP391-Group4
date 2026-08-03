package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.shared.UserDao;
import com.cafe.model.Branch;
import com.cafe.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * A2 · BranchService (đặc tả mục 4).
 */
public class BranchService {

    private final BranchDao dao;
    private final UserDao userDao;

    public BranchService() {
        this(new BranchDao(), new UserDao());
    }

    BranchService(BranchDao dao) {
        this(dao, new UserDao());
    }

    BranchService(BranchDao dao, UserDao userDao) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.userDao = Objects.requireNonNull(userDao, "userDao");
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
        return Tx.call(conn -> {
            int id = dao.insert(conn, b);
            if (id <= 0) throw new SQLException("Không lấy được BranchId sau khi tạo chi nhánh.");
            dao.updateCode(conn, id, String.format("CN%02d", id));
            return id;
        });
    }

    public void updateBranch(Branch b) throws SQLException {
        normalizeAndValidate(b);
        if (b.getBranchId() <= 0) throw new BusinessException("Mã chi nhánh không hợp lệ.");
        Tx.run(conn -> {
            Branch current = dao.findById(conn, b.getBranchId());
            if (current == null) throw new BusinessException("Không tìm thấy chi nhánh cần cập nhật.");
            b.setCode(current.getCode());
            b.setManagerUserId(current.getManagerUserId());
            b.setManagerName(current.getManagerName());
            dao.update(conn, b);
        });
    }

    /** Manager tự cài giờ mở/đóng cửa + ngưỡng cao điểm cho chi nhánh mình (không đụng cột khác). */
    public void updateHoursAndPeak(int branchId, java.time.LocalTime openTime,
                                   java.time.LocalTime closeTime, int peakThresholdCups) throws SQLException {
        Tx.run(conn -> {
            dao.updateHoursAndPeak(conn, branchId, openTime, closeTime, peakThresholdCups);
        });
    }

    public void setBranchActive(int id, boolean active) throws SQLException {
        Tx.run(conn -> {
            dao.updateActive(conn, id, active);
        });
    }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        Tx.run(conn -> {
            Branch b = dao.findById(conn, id);
            if (b == null) throw new BusinessException("Không tìm thấy chi nhánh.");
            dao.updateActive(conn, id, !b.isActive());
        });
    }

    public List<User> getManagerReplacementCandidates(int branchId) throws SQLException {
        if (branchId <= 0) throw new BusinessException("Mã chi nhánh không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            Branch branch = dao.findById(conn, branchId);
            if (branch == null) throw new BusinessException("Không tìm thấy chi nhánh.");
            if (branch.getManagerUserId() == null) {
                throw new BusinessException("Chi nhánh chưa có quản lý; hãy dùng thao tác Phân công.");
            }
            return userDao.findManagerReplacementCandidates(
                    conn, branchId, branch.getManagerUserId());
        }
    }

    /** Thay người phụ trách nhưng không thay đổi BranchId của bất kỳ nhân sự nào. */
    public ManagerReplacement replaceManager(int branchId, int replacementUserId) throws SQLException {
        if (branchId <= 0 || replacementUserId <= 0) {
            throw new BusinessException("Chi nhánh hoặc nhân sự thay thế không hợp lệ.");
        }
        return Tx.call(conn -> {
            Branch branch = dao.findByIdForUpdate(conn, branchId);
            if (branch == null) throw new BusinessException("Không tìm thấy chi nhánh.");
            Integer currentManagerId = branch.getManagerUserId();
            if (currentManagerId == null) {
                throw new BusinessException("Chi nhánh chưa có quản lý; hãy dùng thao tác Phân công.");
            }
            if (currentManagerId == replacementUserId) {
                throw new BusinessException("Nhân sự được chọn đang là quản lý của chi nhánh này.");
            }

            User replacement = userDao.findByIdForUpdate(conn, replacementUserId);
            if (replacement == null) throw new BusinessException("Không tìm thấy nhân sự thay thế.");
            if (!"ACTIVE".equals(replacement.getStatus())
                    || !Objects.equals(replacement.getBranchId(), branchId)
                    || "ADMIN".equals(replacement.getRoleCode())) {
                throw new BusinessException(
                        "Người thay thế phải đang hoạt động và thuộc đúng chi nhánh.");
            }

            User currentManager = userDao.findByIdForUpdate(conn, currentManagerId);
            if (currentManager == null) {
                throw new BusinessException("Không tìm thấy quản lý hiện tại của chi nhánh.");
            }

            requireNoOpenDuty(conn, replacementUserId, "Người thay thế");
            requireNoOpenDuty(conn, currentManagerId, "Quản lý hiện tại");

            userDao.updateRole(conn, replacementUserId, "BRANCH_MANAGER");
            dao.updateManager(conn, branchId, replacementUserId);
            userDao.updateStatus(conn, currentManagerId, "LOCKED");
            return new ManagerReplacement(currentManagerId, replacementUserId);
        });
    }

    private void requireNoOpenDuty(Connection conn, int userId, String subject) throws SQLException {
        String block = userDao.findManagerReplacementBlock(conn, userId);
        if ("CASHIER_SHIFT".equals(block)) {
            throw new BusinessException(subject + " còn ca thu ngân đang mở; hãy đóng ca trước.");
        }
        if ("ATTENDANCE".equals(block)) {
            throw new BusinessException(subject + " đang chấm công; hãy kết thúc ca trước.");
        }
    }

    public record ManagerReplacement(int previousManagerId, int newManagerId) { }

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
