package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.dao.hr.AttendanceDao;
import com.cafe.model.PayrollRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

/**
 * Bảng lương chỉ đọc, tính runtime từ các ShiftAssignment đã APPROVED.
 * Không còn thao tác chốt hoặc sửa tay dòng lương.
 */
public class PayrollService {

    private final AttendanceDao attendanceDao;

    public PayrollService() {
        this(new AttendanceDao());
    }

    PayrollService(AttendanceDao attendanceDao) {
        this.attendanceDao = java.util.Objects.requireNonNull(attendanceDao);
    }

    public List<PayrollRow> getMonthlyPayroll(int branchId, YearMonth payrollMonth)
            throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return attendanceDao.aggregateApprovedByMonth(
                    connection, branchId, payrollMonth.atDay(1));
        }
    }
}
