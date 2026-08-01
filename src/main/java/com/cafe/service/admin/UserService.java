package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.common.PasswordHasher;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchDao;
import com.cafe.dao.admin.UserDao;
import com.cafe.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A1 · UserService (đặc tả mục 4) — quản lý nhân sự (Admin).
 * Mật khẩu mới được băm BCrypt tại Service.
 */
public class UserService {

    private static final BigDecimal MAX_HOURLY_RATE =
            new BigDecimal("9999999999.99");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("[a-z][a-z0-9._-]{3,59}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9]"
                    + "(?:[a-z0-9-]{0,61}[a-z0-9])?"
                    + "(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+");

    private final UserDao dao;
    private final BranchDao branchDao;

    public UserService() {
        this(new UserDao(), new BranchDao());
    }

    UserService(UserDao dao, BranchDao branchDao) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.branchDao = Objects.requireNonNull(branchDao, "branchDao");
    }

    public List<User> getUserList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<User> getUserListByBranch(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByBranch(conn, branchId); }
    }

    /** A2 · danh sách nhân sự có lọc theo vai trò/chi nhánh/từ khoá (null = bỏ qua). */
    public List<User> getUserList(String roleCode, Integer branchId, String q, int offset, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.findFiltered(conn, roleCode, branchId, q, offset, limit);
        }
    }

    public int countUsers(String roleCode, Integer branchId, String q) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.countFiltered(conn, roleCode, branchId, q);
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

    public boolean emailTaken(String email, int excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.emailExists(conn, email, excludeId);
        }
    }

    public boolean phoneTaken(String phone, int excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return dao.phoneExists(conn, phone, excludeId);
        }
    }

    public boolean isBranchManagerRole(String roleCode) {
        return "BRANCH_MANAGER".equals(roleCode);
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
        normalizeAndValidate(u, rawPassword, true);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if ("ADMIN".equals(u.getRoleCode())) {
                    throw new BusinessException(
                            "Hệ thống chỉ có 1 Admin toàn chuỗi, không thể tạo thêm tài khoản Admin.");
                }
                boolean manager = "BRANCH_MANAGER".equals(u.getRoleCode());
                ensureManagerSlot(conn, manager, u.getBranchId(), 0);
                int id = dao.insert(conn, u, PasswordHasher.hashPassword(rawPassword));
                if (id <= 0) throw new BusinessException("Không thể tạo tài khoản nhân viên.");
                if (manager) branchDao.updateManager(conn, u.getBranchId(), id);
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw translateWriteError(e);
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateUser(User u) throws SQLException {
        normalizeAndValidate(u, null, false);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User current = dao.findByIdForUpdate(conn, u.getUserId());
                if (current == null) {
                    throw new BusinessException("Không tìm thấy nhân viên cần cập nhật.");
                }
                if ("ADMIN".equals(current.getRoleCode()) || "ADMIN".equals(u.getRoleCode())) {
                    throw new BusinessException("Tài khoản Admin hệ thống không thể chỉnh sửa.");
                }
                u.setUsername(current.getUsername());
                if (!Objects.equals(current.getBranchId(), u.getBranchId())) {
                    requireBranchTransferAllowed(conn, u.getUserId());
                }
                boolean manager = "BRANCH_MANAGER".equals(u.getRoleCode());
                ensureManagerSlot(conn, manager, u.getBranchId(), u.getUserId());
                if (manager && "LOCKED".equals(u.getStatus())) {
                    throw new BusinessException("Không thể khoá quản lý đang phụ trách chi nhánh.");
                }
                dao.update(conn, u);
                branchDao.clearManagerByUser(conn, u.getUserId());
                if (manager) branchDao.updateManager(conn, u.getBranchId(), u.getUserId());
                conn.commit();
            }
            catch (SQLException e) {
                conn.rollback();
                throw translateWriteError(e);
            }
            catch (RuntimeException e) {
                conn.rollback();
                throw e;
            }
            finally { conn.setAutoCommit(true); }
        }
    }

    private BusinessException translateWriteError(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            int code = current.getErrorCode();
            if (code == 2601 || code == 2627) {
                String message = current.getMessage() == null
                        ? "" : current.getMessage().toLowerCase(Locale.ROOT);
                if (message.contains("ux_useraccount_email")) {
                    return new BusinessException("Email đã được sử dụng bởi nhân sự khác.");
                }
                if (message.contains("ux_useraccount_phone")) {
                    return new BusinessException("Số điện thoại đã được sử dụng bởi nhân sự khác.");
                }
                return new BusinessException("Tên đăng nhập đã tồn tại.");
            }
            if (code == 547) {
                return new BusinessException("Vai trò hoặc chi nhánh đã chọn không còn hợp lệ.");
            }
            if (code == 2628 || code == 8152) {
                return new BusinessException("Thông tin nhập vượt quá độ dài cho phép.");
            }
        }
        return new BusinessException("Không thể lưu nhân sự do dữ liệu không hợp lệ. Vui lòng kiểm tra lại.");
    }

    /** Invariant của tài khoản phải được giữ ở Service để mọi caller đều được bảo vệ. */
    private void normalizeAndValidate(User user, String rawPassword, boolean creating) {
        if (user == null) throw new BusinessException("Thông tin nhân viên là bắt buộc.");
        String username = normalizeLower(user.getUsername());
        String fullName = clean(user.getFullName());
        String email = normalizeLower(user.getEmail());
        String phone = clean(user.getPhone());
        String roleCode = clean(user.getRoleCode());
        String status = clean(user.getStatus());

        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoleCode(roleCode == null ? null : roleCode.toUpperCase(Locale.ROOT));
        user.setStatus(status == null ? "ACTIVE" : status.toUpperCase(Locale.ROOT));
        if (user.getHourlyRate() != null) {
            BigDecimal hourlyRate = user.getHourlyRate();
            if (hourlyRate.signum() < 0 || hourlyRate.compareTo(MAX_HOURLY_RATE) > 0) {
                throw new BusinessException(
                        "Lương theo giờ phải từ 0 đến 9.999.999.999,99₫.");
            }
            try {
                user.setHourlyRate(hourlyRate.setScale(2, RoundingMode.UNNECESSARY));
            } catch (ArithmeticException e) {
                throw new BusinessException("Lương theo giờ chỉ được có tối đa 2 chữ số thập phân.");
            }
        }

        if (creating && user.getUserId() != 0) {
            throw new BusinessException("Tài khoản mới không được có sẵn mã nhân viên.");
        }
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException(
                    "Tên đăng nhập phải có 4-60 ký tự, bắt đầu bằng chữ và chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.");
        }
        if (fullName == null || fullName.length() > 120) {
            throw new BusinessException("Họ tên không được để trống và tối đa 120 ký tự.");
        }
        if (email == null || email.length() > 120 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("Email không đúng định dạng, ví dụ: ten@congty.vn.");
        }
        if (phone == null || !phone.matches("^0\\d{9}$")) {
            throw new BusinessException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
        if (!java.util.Set.of("ADMIN", "BRANCH_MANAGER", "CASHIER", "BARISTA")
                .contains(user.getRoleCode())) {
            throw new BusinessException("Vui lòng chọn vai trò hợp lệ.");
        }
        if (user.getBranchId() == null || user.getBranchId() <= 0) {
            throw new BusinessException("Vui lòng chọn chi nhánh.");
        }
        if (!"ACTIVE".equals(user.getStatus()) && !"LOCKED".equals(user.getStatus())) {
            throw new BusinessException("Trạng thái nhân viên không hợp lệ.");
        }
        if (creating && (rawPassword == null || rawPassword.length() < 6)) {
            throw new BusinessException("Mật khẩu tối thiểu 6 ký tự.");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeLower(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
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

    private void requireBranchTransferAllowed(Connection conn, int userId) throws SQLException {
        String block = dao.findBranchTransferBlock(conn, userId);
        if (block == null) return;
        if ("MANAGER".equals(block))
            throw new BusinessException("Hãy gỡ nhân viên khỏi vị trí quản lý chi nhánh trước khi chuyển.");
        if ("CASHIER_SHIFT".equals(block))
            throw new BusinessException("Nhân viên còn ca thu ngân đang mở; phải đóng ca trước khi chuyển.");
        if ("ATTENDANCE".equals(block))
            throw new BusinessException("Nhân viên đang chấm công và chưa tan ca; phải kết thúc ca trước khi chuyển.");
        throw new BusinessException("Nhân viên còn lịch làm từ hôm nay trở đi; hãy xử lý lịch trước khi chuyển.");
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
        String targetStatus = clean(status);
        if (userId <= 0 || targetStatus == null
                || !("ACTIVE".equalsIgnoreCase(targetStatus) || "LOCKED".equalsIgnoreCase(targetStatus))) {
            throw new BusinessException("Trạng thái nhân viên không hợp lệ.");
        }
        targetStatus = targetStatus.toUpperCase(Locale.ROOT);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User current = dao.findByIdForUpdate(conn, userId);
                if (current == null) throw new BusinessException("Không tìm thấy nhân viên.");
                if ("ADMIN".equals(current.getRoleCode()))
                    throw new BusinessException("Tài khoản Admin luôn hoạt động, không thể khoá.");
                if ("LOCKED".equals(targetStatus) && branchDao.isManagerAssigned(conn, userId))
                    throw new BusinessException("Không thể khoá quản lý đang phụ trách chi nhánh.");
                dao.updateStatus(conn, userId, targetStatus);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void toggleUserStatus(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User current = dao.findByIdForUpdate(conn, userId);
                if (current == null) throw new BusinessException("Không tìm thấy nhân viên.");
                if ("ADMIN".equals(current.getRoleCode()))
                    throw new BusinessException("Tài khoản Admin luôn hoạt động, không thể khoá.");
                String target = "LOCKED".equals(current.getStatus()) ? "ACTIVE" : "LOCKED";
                if ("LOCKED".equals(target) && branchDao.isManagerAssigned(conn, userId))
                    throw new BusinessException("Không thể khoá quản lý đang phụ trách chi nhánh.");
                dao.updateStatus(conn, userId, target);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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
                    if (!java.util.Objects.equals(u.getBranchId(), branchId)) {
                        requireBranchTransferAllowed(conn, userId);
                    }
                    u.setBranchId(branchId);
                    dao.update(conn, u);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
