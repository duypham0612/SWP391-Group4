package com.cafe.service.barista;

import com.cafe.common.BusinessException;
import com.cafe.model.Ingredient;
import com.cafe.model.ShiftClockStatus;
import com.cafe.model.WasteEventItem;
import com.cafe.model.WasteLogLine;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** B5 · WasteService — màn hao hụt nguyên liệu, còn ghi tồn đi qua InventoryService + ledger. */
public class WasteService {
    private static final int MAX_WASTE_ROWS = 20;
    private static final Map<String, Set<String>> PRESETS_BY_TYPE = Map.of(
            "SPILL", Set.of("Đổ khi pha", "Rơi khi thao tác", "Sai định lượng"),
            "EXPIRED", Set.of("Hết hạn", "Nguyên liệu hỏng", "Bảo quản lỗi", "Quá thời gian mở nắp"),
            "OTHER", Set.of("Mẫu thử/QC", "Khác"));
    private static final Map<String, String> INGREDIENT_CAUSES = Map.of(
            "Đổ khi pha", "SPILL", "Rơi khi thao tác", "SPILL", "Sai định lượng", "WRONG_RECIPE",
            "Hết hạn", "EXPIRED", "Nguyên liệu hỏng", "EXPIRED", "Bảo quản lỗi", "STORAGE",
            "Quá thời gian mở nắp", "STORAGE", "Mẫu thử/QC", "QC_SAMPLE", "Khác", "OTHER");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InventoryService inventoryService;
    private final IngredientService ingredientService;
    private final AttendanceService attendanceService;
    private final BranchService branchService;

    public WasteService() {
        this(new InventoryService(), new IngredientService(), new AttendanceService(), new BranchService());
    }

    WasteService(InventoryService inventoryService) {
        this(inventoryService, new IngredientService(), new AttendanceService(), new BranchService());
    }

    WasteService(InventoryService inventoryService, IngredientService ingredientService,
                 AttendanceService attendanceService) {
        this(inventoryService, ingredientService, attendanceService, new BranchService());
    }

    WasteService(InventoryService inventoryService, IngredientService ingredientService,
                 AttendanceService attendanceService, BranchService branchService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.ingredientService = Objects.requireNonNull(ingredientService, "ingredientService");
        this.attendanceService = Objects.requireNonNull(attendanceService, "attendanceService");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
    }

    public List<Ingredient> getIngredients(int branchId) throws SQLException {
        return inventoryService.getActiveWasteIngredients(branchId);
    }

    /**
     * Phạm vi nhật ký hao hụt của người đang xem. Ưu tiên đúng ca của họ; không có ca nào thì rơi
     * về NGÀY KINH DOANH của chi nhánh (mốc giờ mở cửa), không phải nửa đêm lịch — cắt theo nửa đêm
     * thì ca đêm đang chạy bị đứt đôi lúc 00:00 và nửa đầu ca biến mất khỏi bảng.
     */
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
        return WasteScope.businessDay(branchOpenTime(branchId));
    }

    /** Giờ mở cửa chi nhánh; chưa khai hoặc lỗi đọc thì để null → mốc lùi về nửa đêm như trước. */
    private LocalTime branchOpenTime(int branchId) throws SQLException {
        com.cafe.model.Branch branch = branchService.getBranch(branchId);
        return branch == null ? null : branch.getOpenTime();
    }

    /**
     * Nhật ký của màn này CHỈ gồm hao hụt nguyên liệu: dòng do làm lại món sinh ra thuộc về KDS
     * (barista không sửa/huỷ lẻ được) và đã có trong báo cáo đối soát của Quản lý.
     */
    public List<WasteEventItem> getWasteLogs(int branchId, WasteScope scope) throws SQLException {
        return inventoryService.getWasteLogs(branchId, scope.getFromUtc(), scope.getToUtc(), true);
    }

    public InventoryService.WasteLogPage getWasteLogPage(int branchId, WasteScope scope, String query,
                                                          String wasteType, String status, int page, int pageSize) throws SQLException {
        return inventoryService.getWasteLogPage(branchId, scope.getFromUtc(), scope.getToUtc(),
                query, wasteType, status, true, page, pageSize);
    }

    public WasteSummary getTodayWasteSummary(int branchId) throws SQLException {
        return summarize(getWasteLogs(branchId, WasteScope.today()));
    }

    public WasteEventItem getEditableWasteLog(int branchId, long wasteEntryId, int userId) throws SQLException {
        WasteEventItem log = inventoryService.getWasteLog(branchId, wasteEntryId);
        if (log == null) return null;
        if (log.isRemake()) throw new BusinessException("Dòng làm lại món do KDS ghi, không sửa tại màn hao hụt.");
        if (!log.isEditable()) throw new BusinessException("Bản ghi đã hết hạn sửa hoặc đã bị huỷ.");
        if (log.getLoggedBy() != userId) throw new BusinessException("Bạn chỉ được sửa bản ghi do chính mình tạo.");
        if (log.getLoggedAt() == null || log.getLoggedAt().isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(15)))
            throw new BusinessException("Bản ghi đã quá 15 phút, hãy gửi Quản lý đối soát.");
        return log;
    }

    public WasteSummary summarize(List<WasteEventItem> logs) {
        return WasteSummary.from(logs);
    }

    /** {@code requestId} chống gửi trùng khi barista bấm hai lần hoặc mạng chập chờn. */
    public int logIngredientWasteLines(int branchId, List<WasteLogLine> lines, int userId, String requestId) throws SQLException {
        return inventoryService.logWasteLines(branchId, lines, userId, requestId);
    }

    /** Use case ghi batch: chuẩn hóa preset → cause, gộp dòng trùng và chống gửi lặp tại Service. */
    public int logIngredientWasteBatch(int branchId, WasteBatchCommand command, int userId) throws SQLException {
        if (command == null) throw new BusinessException("Dữ liệu hao hụt là bắt buộc.");
        String requestId = requireRequestId(command.clientRequestId());
        List<WasteLineInput> inputs = command.lines() == null ? List.of() : command.lines();
        if (inputs.size() > MAX_WASTE_ROWS)
            throw new BusinessException("Mỗi lần chỉ được ghi tối đa " + MAX_WASTE_ROWS + " dòng hao hụt.");

        Map<WasteLineKey, WasteLogLine> grouped = new LinkedHashMap<>();
        int lineNo = 1;
        for (WasteLineInput input : inputs) {
            if (input == null) { lineNo++; continue; }
            boolean started = !blank(input.ingredientId()) || !blank(input.quantity())
                    || !blank(input.reasonPreset()) || !blank(input.reasonDetail());
            if (!started) { lineNo++; continue; }
            int ingredientId = positiveInt(input.ingredientId(), "Dòng " + lineNo + ": Nguyên liệu không hợp lệ.");
            BigDecimal quantity = positiveQuantity(input.quantity(), "Dòng " + lineNo + ": Số lượng phải > 0.");
            String type = blank(input.wasteType()) ? "OTHER" : input.wasteType().trim().toUpperCase();
            String cause = requireIngredientCause(type, input.reasonPreset(), input.reasonDetail(), lineNo);
            String reason = combineReason(input.reasonPreset(), input.reasonDetail());
            if (blank(reason)) throw new BusinessException("Dòng " + lineNo + ": Vui lòng chọn hoặc nhập lý do.");
            WasteLineKey key = new WasteLineKey(ingredientId, type, cause, reason);
            WasteLogLine existing = grouped.get(key);
            if (existing == null) grouped.put(key,
                    new WasteLogLine(ingredientId, quantity, type, reason, cause));
            else existing.setQuantity(existing.getQuantity().add(quantity));
            lineNo++;
        }
        if (grouped.isEmpty()) throw new BusinessException("Chưa có dòng hao hụt nào để ghi.");
        return inventoryService.logWasteLines(branchId, new ArrayList<>(grouped.values()), userId, requestId);
    }

    /** Lý do phải thuộc đúng preset của loại hao hụt đã chọn. */
    public static String requireIngredientCause(String wasteType, String preset, String detail, int lineNo) {
        String type = wasteType == null ? "" : wasteType.trim().toUpperCase();
        if (!PRESETS_BY_TYPE.containsKey(type))
            throw new BusinessException("Dòng " + lineNo + ": Loại hao hụt không hợp lệ.");
        String chosen = preset == null ? "" : preset.trim();
        if (!PRESETS_BY_TYPE.get(type).contains(chosen))
            throw new BusinessException("Dòng " + lineNo + ": Lý do không phù hợp với loại hao hụt đã chọn.");
        if ("Khác".equals(chosen) && blank(detail))
            throw new BusinessException("Dòng " + lineNo + ": Chọn Khác thì phải nhập diễn giải.");
        return INGREDIENT_CAUSES.get(chosen);
    }

    private static String requireRequestId(String raw) {
        if (raw == null || !raw.trim().matches("[A-Za-z0-9-]{8,60}"))
            throw new BusinessException("Mã gửi không hợp lệ. Vui lòng tải lại màn hình và thử lại.");
        return raw.trim();
    }

    private static int positiveInt(String raw, String message) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) { throw new BusinessException(message); }
    }

    private static BigDecimal positiveQuantity(String raw, String message) {
        try {
            BigDecimal value = new BigDecimal(raw == null ? "" : raw.trim());
            if (value.signum() <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) { throw new BusinessException(message); }
    }

    private static String combineReason(String preset, String detail) {
        String first = blank(preset) ? "" : preset.trim();
        String second = blank(detail) ? "" : detail.trim();
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        return first + " - " + second;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record WasteBatchCommand(String clientRequestId, List<WasteLineInput> lines) { }
    public record WasteLineInput(String ingredientId, String quantity, String wasteType,
                                 String reasonPreset, String reasonDetail) { }
    private record WasteLineKey(int ingredientId, String wasteType, String causeCode, String reason) { }

    /** Sửa dòng hao hụt nguyên liệu — áp txn cho phần chênh lệch. */
    public void updateWaste(int branchId, long wasteEntryId, BigDecimal newQty, String wasteType, String reason, int userId) throws SQLException {
        inventoryService.updateWaste(branchId, wasteEntryId, newQty, wasteType, reason, userId);
    }

    /** Huỷ dòng hao hụt — hoàn kho qua txn bù (không hard-delete). */
    public void voidWaste(int branchId, long wasteEntryId, int userId) throws SQLException {
        inventoryService.voidWaste(branchId, wasteEntryId, userId);
    }

    public static class WasteScope {
        private final String kind;
        private final LocalDateTime fromUtc;
        private final LocalDateTime toUtc;

        private WasteScope(String kind, LocalDateTime fromUtc, LocalDateTime toUtc) {
            this.kind = kind;
            this.fromUtc = fromUtc;
            this.toUtc = toUtc;
        }

        static WasteScope today() {
            LocalDate today = LocalDate.now(VN_ZONE);
            LocalDateTime from = today.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            LocalDateTime to = today.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return new WasteScope("TODAY", from, to);
        }

        /**
         * Ngày kinh doanh hiện tại của chi nhánh: từ giờ mở cửa gần nhất đã trôi qua tới 24h sau.
         * Dùng chung mốc với Quầy pha chế ({@link com.cafe.common.BusinessDay#startUtc}) để hai màn
         * không nói hai chuyện khác nhau về "hôm nay". openTime null → lùi về nửa đêm.
         */
        static WasteScope businessDay(LocalTime openTime) {
            if (openTime == null) return today();
            LocalDateTime from = com.cafe.common.BusinessDay.startUtc(openTime);
            return new WasteScope("BUSINESS_DAY", from, from.plusDays(1));
        }

        static WasteScope openShift(LocalDateTime checkInUtc) {
            return new WasteScope("OPEN_SHIFT", checkInUtc, null);
        }

        static WasteScope closedShift(LocalDateTime checkInUtc, LocalDateTime checkOutUtc) {
            return new WasteScope("CLOSED_SHIFT", checkInUtc, checkOutUtc);
        }

        public String getKind() { return kind; }
        public LocalDateTime getFromUtc() { return fromUtc; }
        public LocalDateTime getToUtc() { return toUtc; }

    }

}
