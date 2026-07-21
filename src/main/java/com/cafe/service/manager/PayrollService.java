package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.config.DBConnection;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.dao.manager.PayrollDao;
import com.cafe.model.Payroll;
import com.cafe.model.PayrollRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** M4 · PayrollService — bảng lương tháng: giờ từ chấm công APPROVED + override giờ/lương đã chốt (hr.Payroll). */
public class PayrollService {

    private final AttendanceDao dao = new AttendanceDao();
    private final PayrollDao payrollDao = new PayrollDao();

    /**
     * Bảng lương tháng: lấy giờ tính từ chấm công APPROVED làm mặc định, overlay giờ/lương Manager đã chốt.
     * @param payMonth chuỗi 'yyyy-MM'
     */
    public List<PayrollRow> getMonthlyPayroll(int branchId, LocalDate monthStart, String payMonth) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<PayrollRow> rows = dao.aggregateApprovedByMonth(c, branchId, monthStart.withDayOfMonth(1));
            Map<Integer, Payroll> saved = payrollDao.findByMonth(c, branchId, payMonth);
            for (PayrollRow r : rows) {
                r.setComputedHours(r.getTotalHours());
                Payroll p = saved.get(r.getUserId());
                if (p != null) {
                    if (p.getWorkedHours() != null) r.setTotalHours(p.getWorkedHours().doubleValue());
                    r.setHourlyRate(p.getHourlyRate());
                    r.setOverridden(true);
                }
            }
            return rows;
        }
    }

    /** Chốt/sửa lương: upsert từng nhân viên (giờ + lương/giờ) cho tháng — 1 transaction. */
    public void savePayroll(int branchId, String payMonth, List<Payroll> lines, int updatedBy) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        validateHourlyRates(lines);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Payroll p : lines) {
                    if (!isActiveBranchUser(c, p.getUserId(), branchId)) throw new BusinessException("Nhân viên không thuộc chi nhánh của bạn hoặc đã bị khóa.");
                    payrollDao.upsert(c, branchId, p.getUserId(), payMonth, p.getWorkedHours(), p.getHourlyRate(), updatedBy);
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    static void validateHourlyRates(List<Payroll> lines) {
        for (Payroll p : lines) {
            if (p.getWorkedHours() == null || p.getWorkedHours().signum() < 0 || p.getWorkedHours().compareTo(new BigDecimal("744")) > 0)
                throw new BusinessException("Số giờ làm phải trong khoảng 0 đến 744 giờ/tháng.");
            BigDecimal hourlyRate = p.getHourlyRate();
            if (hourlyRate == null || hourlyRate.compareTo(Constants.MIN_HOURLY_RATE) < 0) {
                throw new BusinessException("Lương cơ bản phải lớn hơn hoặc bằng 25.000₫/giờ.");
            }
        }
    }

    private boolean isActiveBranchUser(Connection c, int userId, int branchId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM iam.[User] WHERE UserId=? AND BranchId=? AND Status='ACTIVE'")) {
            ps.setInt(1, userId); ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
