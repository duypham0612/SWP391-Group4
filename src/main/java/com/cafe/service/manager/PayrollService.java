package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.model.PayrollRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** M4 · PayrollService — bảng lương tháng (tổng giờ từ chấm công APPROVED). */
public class PayrollService {

    private final AttendanceDao dao = new AttendanceDao();

    /** @param month yyyy-MM (ví dụ "2026-06"); null = tháng hiện tại theo today truyền vào. */
    public List<PayrollRow> getMonthlyPayroll(int branchId, LocalDate monthStart) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return dao.aggregateApprovedByMonth(c, branchId, monthStart.withDayOfMonth(1));
        }
    }
}
