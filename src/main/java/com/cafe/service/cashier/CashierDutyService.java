package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.dao.payment.CashierShiftDao;
import com.cafe.dao.hr.AttendanceDao;
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

    private final AttendanceDao attendanceDao;
    private final AttendanceService attendanceService;
    private final CashierShiftDao cashierShiftDao;
    private final CashierShiftService cashierShiftService;

    public CashierDutyService() {
        this(new AttendanceDao(), new AttendanceService(), new CashierShiftDao(), new CashierShiftService());
    }
    public CashierDutyService(AttendanceDao attendanceDao, AttendanceService attendanceService,
                              CashierShiftDao cashierShiftDao, CashierShiftService cashierShiftService) {
        this.attendanceDao = java.util.Objects.requireNonNull(attendanceDao);
        this.attendanceService = java.util.Objects.requireNonNull(attendanceService);
        this.cashierShiftDao = java.util.Objects.requireNonNull(cashierShiftDao);
        this.cashierShiftService = java.util.Objects.requireNonNull(cashierShiftService);
    }

    public DutyState getDutyState(int userId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            boolean clockedIn = isClockedIn(c, userId, branchId);
            boolean tillOpen = cashierShiftDao.findOpenByCashier(c, userId, branchId) != null;
            if (clockedIn && tillOpen) return DutyState.ON_DUTY;
            if (clockedIn) return DutyState.CLOCKED_NO_TILL;
            if (tillOpen) return DutyState.TILL_ONLY;
            return DutyState.OFF_DUTY;
        }
    }

    /** Bắt đầu ca = vào ca chấm công + mở két trong cùng transaction. */
    public int startDuty(int userId, int branchId, BigDecimal openingCash) throws SQLException {
        CashierCashReconciliation.requireValidMoney(openingCash, "Quỹ đầu ca");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = cashierShiftService.openShift(c, branchId, userId, openingCash);
                attendanceService.clockIn(c, userId, branchId);
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
    public void closeDuty(int userId, int branchId, int shiftId, BigDecimal closingCash,
                          boolean handoverConfirmed) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                cashierShiftService.closeShift(
                        c, shiftId, userId, branchId, closingCash, handoverConfirmed);
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
        // Cùng cửa sổ ca với chấm công để ca đêm sau nửa đêm vẫn được coi là đang trực.
        List<ShiftAssignment> assignments = attendanceService.currentShiftAssignments(c, userId, branchId);
        for (ShiftAssignment assignment : assignments) {
            ShiftAssignment attendance = attendanceDao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getAttendanceStatus() != null
                    && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                return true;
            }
        }
        return false;
    }
}
