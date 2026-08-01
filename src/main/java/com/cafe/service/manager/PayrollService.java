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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** M4 · PayrollService — bảng lương tháng: giờ từ chấm công APPROVED + override giờ/lương đã chốt (hr.Payroll). */
public class PayrollService {

    private final AttendanceDao dao;
    private final PayrollDao payrollDao;

    public PayrollService() { this(new AttendanceDao(), new PayrollDao()); }
    public PayrollService(AttendanceDao dao, PayrollDao payrollDao) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.payrollDao = java.util.Objects.requireNonNull(payrollDao);
    }

    /**
     * Bảng lương tháng: lấy giờ tính từ chấm công APPROVED làm mặc định, overlay giờ/lương Manager đã chốt.
     * @param payrollMonth tháng nghiệp vụ; DAO luôn bind xuống DATE ngày đầu tháng.
     */
    public List<PayrollRow> getMonthlyPayroll(int branchId, YearMonth payrollMonth) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<PayrollRow> rows = dao.aggregateApprovedByMonth(c, branchId, payrollMonth.atDay(1));
            Map<Integer, Payroll> saved = payrollDao.findByMonth(c, branchId, payrollMonth);
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
    public void savePayroll(int branchId, YearMonth payrollMonth, List<Payroll> lines, int updatedBy) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        validateWorkedHours(lines);
        validateHourlyRates(lines);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Payroll p : lines) {
                    if (!payrollDao.upsert(c, branchId, p.getUserId(), payrollMonth,
                            p.getWorkedHours(), p.getHourlyRate(), updatedBy)) {
                        throw new BusinessException(
                                "Nhân viên không hoạt động tại chi nhánh hiện tại.");
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    static void validateHourlyRates(List<Payroll> lines) {
        for (Payroll p : lines) {
            BigDecimal hourlyRate = p.getHourlyRate();
            if (hourlyRate == null || hourlyRate.compareTo(Constants.MIN_HOURLY_RATE) < 0) {
                throw new BusinessException("Lương cơ bản phải lớn hơn hoặc bằng 25.000₫/giờ.");
            }
        }
    }

    static void validateWorkedHours(List<Payroll> lines) {
        for (Payroll p : lines) {
            BigDecimal hours = p.getWorkedHours();
            if (hours == null
                    || hours.compareTo(Constants.MIN_WORKED_HOURS_EXCLUSIVE) <= 0
                    || hours.remainder(Constants.WORKED_HOURS_STEP).compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException("Số giờ làm phải lớn hơn 5 giờ và chia hết cho 5.");
            }
        }
    }
}
