package com.cafe.service.barista;

import com.cafe.model.WasteLog;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B5 · Test tổng hợp hao hụt/làm lại (WasteSummary.from) — logic thuần, không đụng DB.
 * Kiểm: bỏ dòng VOIDED, tách hao hụt nguyên liệu vs làm lại, dòng thiếu giá, top nguyên liệu tốn nhất.
 */
class WasteSummaryTest {

    /** Tạo 1 dòng waste. unitCost = null → coi như chưa có giá. */
    private static WasteLog log(String type, String status, String ingName, int ingId,
                                String qty, String unitCost) {
        WasteLog w = new WasteLog();
        w.setWasteType(type);
        w.setStatus(status);
        w.setIngredientName(ingName);
        w.setIngredientId(ingId);
        if (qty != null) w.setQuantity(new BigDecimal(qty));
        if (unitCost != null) w.setUnitCost(new BigDecimal(unitCost));
        return w;
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "expected " + expected + " but was " + actual);
    }

    /** Danh sách rỗng/null → tất cả 0, không có top. */
    @Test
    void empty_list_yields_zeroes() {
        WasteSummary s = WasteSummary.from(null);
        assertEquals(0, s.getActiveCount());
        assertEquals(0, s.getIngredientWasteCount());
        assertEquals(0, s.getRemakeCount());
        assertEquals(0, s.getMissingCostCount());
        assertMoney(s.getTotalCost(), "0");
        assertFalse(s.isHasTopIngredient());
    }

    /** Dòng VOIDED không được tính vào bất kỳ tổng nào. */
    @Test
    void voided_log_is_ignored() {
        WasteSummary s = WasteSummary.from(List.of(
                log("SPILL", "VOIDED", "Sữa tươi", 2, "3", "1000")));
        assertEquals(0, s.getActiveCount());
        assertMoney(s.getTotalCost(), "0");
    }

    /** Tách rõ hao hụt nguyên liệu vs làm lại (REMAKE) theo cả số dòng lẫn chi phí. */
    @Test
    void splits_ingredient_waste_and_remake() {
        WasteSummary s = WasteSummary.from(List.of(
                log("SPILL",  "ACTIVE", "Sữa tươi", 2, "2", "1000"),   // 2000 hao hụt
                log("REMAKE", "ACTIVE", "Cà phê",   1, "1", "5000")));  // 5000 làm lại
        assertEquals(2, s.getActiveCount());
        assertEquals(1, s.getIngredientWasteCount());
        assertEquals(1, s.getRemakeCount());
        assertMoney(s.getIngredientWasteCost(), "2000");
        assertMoney(s.getRemakeCost(), "5000");
        assertMoney(s.getTotalCost(), "7000");
    }

    /** Một lần remake có nhiều nguyên liệu chỉ được tính là một lần/ly trên KPI mới. */
    @Test
    void remake_event_is_counted_once_even_when_it_has_many_ingredients() {
        WasteLog coffee = log("REMAKE", "ACTIVE", "Cà phê", 1, "1", "5000"); coffee.setWasteEventId(91L);
        WasteLog milk = log("REMAKE", "ACTIVE", "Sữa", 2, "2", "1000"); milk.setWasteEventId(91L);
        WasteSummary s = WasteSummary.from(List.of(coffee, milk));
        assertEquals(1, s.getRemakeCount());
        assertMoney(s.getRemakeCost(), "7000");
    }

    @Test
    void snapshot_cost_wins_over_later_estimate() {
        WasteLog w = log("SPILL", "ACTIVE", "Sữa", 2, "2", "9999");
        w.setCostBasis("SNAPSHOT"); w.setUnitCostAtLog(new BigDecimal("1000"));
        WasteSummary s = WasteSummary.from(List.of(w));
        assertMoney(s.getTotalCost(), "2000");
    }

    /** Dòng thiếu giá (unitCost null) → đếm missingCost, KHÔNG cộng vào totalCost. */
    @Test
    void missing_cost_counted_but_not_summed() {
        WasteSummary s = WasteSummary.from(List.of(
                log("SPILL", "ACTIVE", "Đá viên", 4, "10", null),      // thiếu giá
                log("SPILL", "ACTIVE", "Sữa tươi", 2, "1", "1000")));   // 1000
        assertEquals(2, s.getActiveCount());
        assertEquals(1, s.getMissingCostCount());
        assertMoney(s.getTotalCost(), "1000");
    }

    /** Top nguyên liệu = tốn nhất sau khi gộp nhiều dòng cùng tên. */
    @Test
    void top_ingredient_is_the_costliest_aggregated() {
        WasteSummary s = WasteSummary.from(List.of(
                log("SPILL", "ACTIVE", "Sữa tươi", 2, "1", "1000"),    // Sữa 1000
                log("SPILL", "ACTIVE", "Sữa tươi", 2, "1", "1000"),    // Sữa +1000 = 2000
                log("SPILL", "ACTIVE", "Cà phê",   1, "1", "1500")));   // Cà phê 1500
        assertTrue(s.isHasTopIngredient());
        assertEquals("Sữa tươi", s.getTopIngredientName());  // 2000 > 1500
        assertMoney(s.getTopIngredientCost(), "2000");
    }

    /** getTodayWasteSummary dùng cửa sổ hôm nay rồi gom như summary thường. */
    @Test
    void today_summary_uses_today_scope_and_summarizes_logs() throws Exception {
        FakeInventoryService inventory = new FakeInventoryService(List.of(
                log("SPILL", "ACTIVE", "Sữa tươi", 2, "2", "1000"),
                log("REMAKE", "ACTIVE", "Cà phê", 1, "1", "5000"),
                log("SPILL", "VOIDED", "Đá viên", 4, "10", "100")));
        WasteService service = new WasteService(inventory);

        WasteSummary s = service.getTodayWasteSummary(7);

        assertEquals(7, inventory.branchId);
        assertTrue(inventory.fromUtc != null);
        assertTrue(inventory.toUtc != null);
        assertEquals(24, Duration.between(inventory.fromUtc, inventory.toUtc).toHours());
        assertEquals(2, s.getActiveCount());
        assertEquals(1, s.getIngredientWasteCount());
        assertEquals(1, s.getRemakeCount());
        assertMoney(s.getTotalCost(), "7000");
        assertEquals("Cà phê", s.getTopIngredientName());
    }

    private static class FakeInventoryService extends InventoryService {
        private final List<WasteLog> logs;
        private int branchId;
        private LocalDateTime fromUtc;
        private LocalDateTime toUtc;

        FakeInventoryService(List<WasteLog> logs) {
            this.logs = logs;
        }

        @Override
        public List<WasteLog> getWasteLogs(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc)
                throws SQLException {
            this.branchId = branchId;
            this.fromUtc = fromUtc;
            this.toUtc = toUtc;
            return logs;
        }
    }
}
