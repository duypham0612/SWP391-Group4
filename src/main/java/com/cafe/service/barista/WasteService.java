package com.cafe.service.barista;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.Ingredient;
import com.cafe.model.ShiftClockStatus;
import com.cafe.model.WasteLog;
import com.cafe.model.WasteLogLine;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** B5 · WasteService — màn hao hụt/làm lại, còn ghi tồn đi qua InventoryService + ledger. */
public class WasteService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InventoryService inventoryService;
    private final IngredientService ingredientService;
    private final AttendanceService attendanceService;
    private final BranchMenuDao branchMenuDao;

    public WasteService() {
        this(new InventoryService());
    }

    WasteService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.ingredientService = new IngredientService();
        this.attendanceService = new AttendanceService();
        this.branchMenuDao = new BranchMenuDao();
    }

    public List<Ingredient> getIngredients() throws SQLException {
        return ingredientService.getIngredientList();
    }

    public List<BranchMenuItem> getRemakeProducts(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<BranchMenuItem> out = new ArrayList<>();
            for (BranchMenuItem item : branchMenuDao.listForBranch(conn, branchId)) {
                if (item.isPublished()) out.add(item);
            }
            return out;
        }
    }

    public WasteScope resolveScope(int userId, int branchId) throws SQLException {
        if (userId > 0) {
            ShiftClockStatus clock = attendanceService.getMyShiftStatus(userId, branchId, LocalDate.now(VN_ZONE));
            if (clock != null && clock.isCanClockOut() && clock.getCheckInAt() != null) {
                return WasteScope.openShift(clock.getCheckInAt());
            }
            if (clock != null && clock.isClockedOut() && clock.getCheckInAt() != null && clock.getCheckOutAt() != null) {
                return WasteScope.closedShift(clock.getCheckInAt(), clock.getCheckOutAt());
            }
        }
        return WasteScope.today();
    }

    public List<WasteLog> getWasteLogs(int branchId) throws SQLException {
        return inventoryService.getWasteLogs(branchId);
    }

    public List<WasteLog> getWasteLogs(int branchId, WasteScope scope) throws SQLException {
        return inventoryService.getWasteLogs(branchId, scope.getFromUtc(), scope.getToUtc());
    }

    public InventoryService.WasteLogPage getWasteLogPage(int branchId, WasteScope scope, String query,
                                                          String wasteType, String status, int page, int pageSize) throws SQLException {
        return inventoryService.getWasteLogPage(branchId, scope.getFromUtc(), scope.getToUtc(),
                query, wasteType, status, page, pageSize);
    }

    public WasteSummary getTodayWasteSummary(int branchId) throws SQLException {
        return summarize(getWasteLogs(branchId, WasteScope.today()));
    }

    public WasteLog getEditableWasteLog(int branchId, int wasteLogId) throws SQLException {
        WasteLog log = inventoryService.getWasteLog(branchId, wasteLogId);
        if (log == null) return null;
        if (!log.isEditable()) throw new BusinessException("Dòng làm lại món không sửa lẻ; hãy huỷ rồi ghi lại nếu cần.");
        return log;
    }

    public WasteSummary summarize(List<WasteLog> logs) {
        return WasteSummary.from(logs);
    }

    public int logIngredientWasteLines(int branchId, List<WasteLogLine> lines, int userId) throws SQLException {
        return inventoryService.logWasteLines(branchId, lines, userId);
    }

    public int logWaste(int branchId, int ingredientId, BigDecimal qty, String wasteType, String reason, int userId) throws SQLException {
        return inventoryService.logWaste(branchId, ingredientId, qty, wasteType, reason, userId);
    }

    public int remakeProduct(int branchId, int productId, int qty, List<Integer> optionIds, String reason, int userId) throws SQLException {
        return inventoryService.remakeProduct(branchId, productId, qty, optionIds, reason, userId);
    }

    /** JSON tuỳ chọn (có tác động nguyên liệu) theo món cho form làm lại: {productId:[{id,name}]}. */
    public String getRemakeModifiersJson(List<BranchMenuItem> products) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        if (products != null) for (BranchMenuItem p : products) ids.add(p.getProductId());
        return inventoryService.getRemakeModifiersJson(ids);
    }

    /** Sửa dòng hao hụt nguyên liệu — áp txn cho phần chênh lệch. */
    public void updateWaste(int branchId, int wasteLogId, BigDecimal newQty, String wasteType, String reason, int userId) throws SQLException {
        inventoryService.updateWaste(branchId, wasteLogId, newQty, wasteType, reason, userId);
    }

    /** Huỷ dòng hao hụt/remake — hoàn kho qua txn bù (không hard-delete). */
    public void voidWaste(int branchId, int wasteLogId, int userId) throws SQLException {
        inventoryService.voidWaste(branchId, wasteLogId, userId);
    }

    public static class WasteScope {
        private final String kind;
        private final String label;
        private final LocalDateTime fromUtc;
        private final LocalDateTime toUtc;

        private WasteScope(String kind, String label, LocalDateTime fromUtc, LocalDateTime toUtc) {
            this.kind = kind;
            this.label = label;
            this.fromUtc = fromUtc;
            this.toUtc = toUtc;
        }

        static WasteScope today() {
            LocalDate today = LocalDate.now(VN_ZONE);
            LocalDateTime from = today.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            LocalDateTime to = today.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return new WasteScope("TODAY", "Hôm nay", from, to);
        }

        static WasteScope openShift(LocalDateTime checkInUtc) {
            return new WasteScope("OPEN_SHIFT", "Ca đang mở", checkInUtc, null);
        }

        static WasteScope closedShift(LocalDateTime checkInUtc, LocalDateTime checkOutUtc) {
            return new WasteScope("CLOSED_SHIFT", "Ca vừa tan", checkInUtc, checkOutUtc);
        }

        public String getKind() { return kind; }
        public String getLabel() { return label; }
        public LocalDateTime getFromUtc() { return fromUtc; }
        public LocalDateTime getToUtc() { return toUtc; }

        public String getWindowDisplay() {
            if ("TODAY".equals(kind)) {
                return LocalDate.now(VN_ZONE).format(DATE_FMT);
            }
            String from = formatUtc(fromUtc);
            if (toUtc == null) return "Từ " + from;
            return from + " - " + formatUtc(toUtc);
        }

        private static String formatUtc(LocalDateTime value) {
            if (value == null) return "";
            return value.atZone(ZoneOffset.UTC).withZoneSameInstant(VN_ZONE).format(DATE_TIME_FMT);
        }
    }

}
