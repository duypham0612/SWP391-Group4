package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.common.BusinessDay;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.model.Attendance;
import com.cafe.model.CashierShift;
import com.cafe.model.ShiftAssignment;
import com.cafe.service.manager.AttendanceService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Nguồn sự thật trạng thái trực ca thu ngân: chấm công + két tiền. */
public class CashierDutyService {

    public enum DutyState {
        OFF_DUTY,
        CLOCKED_NO_TILL,
        ON_DUTY,
        TILL_ONLY
    }

    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final AttendanceService attendanceService = new AttendanceService();
    private final CashierShiftDao cashierShiftDao = new CashierShiftDao();

    public DutyState getDutyState(int userId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            boolean clockedIn = isClockedIn(c, userId, branchId);
            boolean tillOpen = cashierShiftDao.findOpenByCashier(c, userId) != null;
            if (clockedIn && tillOpen) return DutyState.ON_DUTY;
            if (clockedIn) return DutyState.CLOCKED_NO_TILL;
            if (tillOpen) return DutyState.TILL_ONLY;
            return DutyState.OFF_DUTY;
        }
    }

    /** Bắt đầu ca = vào ca chấm công + mở két trong cùng transaction. */
    public int startDuty(int userId, int branchId, BigDecimal openingCash) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                attendanceService.clockIn(c, userId, branchId);
                CashierShift open = cashierShiftDao.findOpenByCashier(c, userId);
                int id = open != null
                        ? open.getCashierShiftId()
                        : cashierShiftDao.insertOpen(c, branchId, userId, openingCash);
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Kết ca = đóng két + tan ca trong cùng transaction. */
    public void closeDuty(int userId, int branchId, int shiftId, BigDecimal closingCash) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                CashierShift open = cashierShiftDao.findOpenByCashier(c, userId);
                if (open == null || open.getCashierShiftId() != shiftId) {
                    throw new IllegalStateException("Không tìm thấy ca két đang mở của bạn.");
                }
                cashierShiftDao.close(c, shiftId, closingCash);
                attendanceService.clockOut(c, userId, branchId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private boolean isClockedIn(Connection c, int userId, int branchId) throws SQLException {
        List<ShiftAssignment> assignments = attendanceDao.findTodayAssignments(c, userId, branchId, BusinessDay.todayVn());
        for (ShiftAssignment assignment : assignments) {
            Attendance attendance = attendanceDao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                return true;
            }
        }
        return false;
    }
}
