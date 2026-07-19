package com.cafe.service.manager;

import com.cafe.common.BusinessDay;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.dao.shared.BranchInventoryDao;
import com.cafe.dao.manager.ShiftAssignmentDao;
import com.cafe.model.BranchInventory;
import com.cafe.model.MenuBlockRequest;
import com.cafe.model.ShiftAssignment;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** M1 · ManagerDashboardService — tổng hợp chỉ số chi nhánh. */
public class ManagerDashboardService {

    private final BranchInventoryDao biDao = new BranchInventoryDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final ShiftAssignmentDao assignmentDao = new ShiftAssignmentDao();
    private final BillDao billDao = new BillDao();
    private final BranchMenuService branchMenuService = new BranchMenuService();
    private final InventoryService inventoryService = new InventoryService();

    /** Cảnh báo tồn thấp (QuantityOnHand <= MinThreshold). */
    public List<BranchInventory> getLowStockAlerts(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<BranchInventory> rows = new ArrayList<>(biDao.findLowStock(c, branchId));
            rows.removeIf(BranchInventory::isOversold);
            return rows;
        }
    }

    /** Tồn âm đang còn ở số dư hiện tại; stock.oversold trong outbox chỉ giữ dấu vết lịch sử. */
    public List<BranchInventory> getOversoldAlerts(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return biDao.findOversold(c, branchId); }
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

    /** Yêu cầu tạm hết đang chờ manager xử lý. DAO đã xếp món quá hạn lên đầu. */
    public List<MenuBlockRequest> getOpenMenuBlockRequests(int branchId) throws SQLException {
        return branchMenuService.getOpenRequests(branchId);
    }

    /** Hao hụt + làm lại của ngày VN hôm nay (WasteLog.LoggedAt lưu UTC nên phải quy đổi). */
    public WasteSummary getTodayWasteSummary(int branchId, LocalDate todayVn) throws SQLException {
        LocalDateTime fromUtc = BusinessDay.vnDayStartUtc(todayVn);
        LocalDateTime toUtc = BusinessDay.vnDayEndExclusiveUtc(todayVn);
        return WasteSummary.from(inventoryService.getWasteLogs(branchId, fromUtc, toUtc));
    }

    /** Gói số liệu tổng quan cho thẻ thống kê. */
    public Summary getTodaySummary(int branchId, LocalDate today) throws SQLException {
        Summary s = new Summary();
        s.lowStockCount = getLowStockAlerts(branchId).size();
        s.oversoldCount = getOversoldAlerts(branchId).size();
        s.pendingApprovals = getPendingApprovals(branchId);
        s.staffOnShift = getStaffOnShift(branchId, today).size();
        s.todayRevenue = getTodayRevenue(branchId);
        s.openMenuBlocks = getOpenMenuBlockRequests(branchId);
        s.todayWaste = getTodayWasteSummary(branchId, today);
        return s;
    }

    public static class Summary {
        public int lowStockCount;
        public int oversoldCount;
        public int pendingApprovals;
        public int staffOnShift;
        public BigDecimal todayRevenue = BigDecimal.ZERO;
        public List<MenuBlockRequest> openMenuBlocks = List.of();
        public WasteSummary todayWaste = WasteSummary.from(List.of());

        public int getLowStockCount() { return lowStockCount; }
        public int getOversoldCount() { return oversoldCount; }
        public boolean isHasOversold() { return oversoldCount > 0; }
        public int getPendingApprovals() { return pendingApprovals; }
        public int getStaffOnShift() { return staffOnShift; }
        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public List<MenuBlockRequest> getOpenMenuBlocks() { return openMenuBlocks; }
        public int getOpenMenuBlockCount() { return openMenuBlocks == null ? 0 : openMenuBlocks.size(); }
        public boolean isHasOpenMenuBlocks() { return getOpenMenuBlockCount() > 0; }
        public WasteSummary getTodayWaste() { return todayWaste; }

        public int getOverdueMenuBlockCount() {
            int n = 0;
            if (openMenuBlocks == null) return 0;
            for (MenuBlockRequest r : openMenuBlocks) if (r != null && r.isOverdue()) n++;
            return n;
        }
    }
}
