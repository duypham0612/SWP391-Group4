package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.dao.shared.BranchInventoryDao;
import com.cafe.dao.manager.ShiftAssignmentDao;
import com.cafe.model.BranchInventory;
import com.cafe.model.ShiftAssignment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** M1 · ManagerDashboardService — tổng hợp chỉ số chi nhánh. */
public class ManagerDashboardService {

    private final BranchInventoryDao biDao = new BranchInventoryDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final ShiftAssignmentDao assignmentDao = new ShiftAssignmentDao();
    private final BillDao billDao = new BillDao();

    /** Cảnh báo tồn thấp (QuantityOnHand <= MinThreshold). */
    public List<BranchInventory> getLowStockAlerts(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return biDao.findLowStock(c, branchId); }
    }

    /** Số chấm công đang chờ duyệt. */
    public int getPendingApprovals(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return attendanceDao.countByStatus(c, branchId, "PENDING"); }
    }

    /** Nhân viên có ca hôm nay. */
    public List<ShiftAssignment> getStaffOnShift(int branchId, LocalDate today) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            // findByBranchAndWeek với weekStart=today, lọc đúng hôm nay
            List<ShiftAssignment> week = assignmentDao.findByBranchAndWeek(c, branchId, today);
            week.removeIf(a -> a.getWorkDate() == null || !a.getWorkDate().isEqual(today));
            return week;
        }
    }

    /** Doanh thu PAID hôm nay của chi nhánh. */
    public BigDecimal getTodayRevenue(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.sumPaidToday(c, branchId); }
    }

    /** Gói số liệu tổng quan cho thẻ thống kê. */
    public Summary getTodaySummary(int branchId, LocalDate today) throws SQLException {
        Summary s = new Summary();
        s.lowStockCount = getLowStockAlerts(branchId).size();
        s.pendingApprovals = getPendingApprovals(branchId);
        s.staffOnShift = getStaffOnShift(branchId, today).size();
        s.todayRevenue = getTodayRevenue(branchId);
        return s;
    }

    public static class Summary {
        public int lowStockCount;
        public int pendingApprovals;
        public int staffOnShift;
        public BigDecimal todayRevenue = BigDecimal.ZERO;
        public int getLowStockCount() { return lowStockCount; }
        public int getPendingApprovals() { return pendingApprovals; }
        public int getStaffOnShift() { return staffOnShift; }
        public BigDecimal getTodayRevenue() { return todayRevenue; }
    }
}
