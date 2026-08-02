package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * KHOÁ mã + nhãn của lý do báo sự cố và làm lại món.
 *
 * <p>Vì sao cần khoá: nhãn tiếng Việt ở hai enum này được ghi THẲNG vào nhật ký món và sổ hao hụt.
 * Sửa một ký tự là dữ liệu lịch sử trước/sau lệch nhau, mà không có gì khác bắt được — đọc báo cáo
 * đối soát sẽ thấy hai lý do khác nhau cho cùng một sự việc.
 *
 * <p>Thứ tự cũng bị khoá: đây chính là thứ tự hiện trên dropdown của {@code views/barista/kds.jsp}.
 *
 * <p>Test này ĐỎ khi ai đó sửa nhãn/mã/thứ tự. Nếu việc sửa là cố ý, cập nhật danh sách dưới đây
 * trong cùng commit — coi như một lần xác nhận có ý thức, không phải sửa cho hết đỏ.
 */
class ReasonLabelLockTest {

    @Test
    void issue_reason_codes_and_labels_are_locked() {
        assertEquals(List.of(
                "OUT_OF_STOCK=Hết nguyên liệu",
                "EQUIPMENT=Máy móc gặp sự cố",
                "NOTE_UNSUPPORTED=Không đáp ứng được ghi chú",
                "DISCONTINUED=Món đã ngừng bán",
                "UNCLEAR_ORDER=Thông tin đơn không rõ",
                "OTHER=Lý do khác"), describe(IssueReason.values()));
    }

    @Test
    void remake_reason_codes_and_labels_are_locked() {
        assertEquals(List.of(
                "WRONG_RECIPE=Pha sai công thức",
                "SPILLED=Làm đổ hoặc hư món",
                "QUALITY=Chất lượng không đạt",
                "CUSTOMER_FEEDBACK=Khách phản hồi",
                "WRONG_DELIVERY=Giao nhầm",
                "CHANGED_REQUEST=Khách thay đổi yêu cầu"), describe(RemakeReason.values()));
    }

    /**
     * Chỉ đúng hai lý do được phép chặn món. Thêm nhầm cờ {@code blocking} cho một lý do khác là
     * món rời hàng chờ mà barista không hiểu tại sao.
     */
    @Test
    void only_equipment_and_discontinued_block_the_item() {
        List<String> blocking = new ArrayList<>();
        for (IssueReason reason : IssueReason.values()) {
            if (reason.isBlocking()) blocking.add(reason.name());
        }
        assertEquals(List.of("EQUIPMENT", "DISCONTINUED"), blocking);
        assertEquals("EQUIPMENT,DISCONTINUED", IssueReason.blockingCodesCsv());
    }

    /** OUT_OF_STOCK đi nhánh riêng (sửa sổ kho) nên KHÔNG được mang cờ blocking. */
    @Test
    void out_of_stock_is_not_flagged_blocking() {
        assertFalse(IssueReason.OUT_OF_STOCK.isBlocking());
    }

    @Test
    void from_code_trims_and_ignores_case_but_rejects_unknown() {
        assertEquals(IssueReason.EQUIPMENT, IssueReason.fromCode("  equipment "));
        assertEquals(RemakeReason.SPILLED, RemakeReason.fromCode("spilled"));
        assertNull(IssueReason.fromCode("KHONG_CO_MA_NAY"));
        assertNull(IssueReason.fromCode(""));
        assertNull(IssueReason.fromCode(null));
        assertNull(RemakeReason.fromCode("OTHER"));   // OTHER chỉ có ở IssueReason
    }

    /** Dropdown phải liệt kê đủ, không lọc bớt như Reason86 — mọi lý do đều barista chọn được. */
    @Test
    void selectable_values_expose_every_constant() {
        assertEquals(IssueReason.values().length, IssueReason.selectableValues().size());
        assertEquals(RemakeReason.values().length, RemakeReason.selectableValues().size());
    }

    /** Nhãn không được rỗng — JSP in thẳng ra option, rỗng là dropdown có dòng trắng. */
    @Test
    void every_label_is_present() {
        for (IssueReason reason : IssueReason.values()) {
            assertTrue(reason.label() != null && !reason.label().isBlank(), reason.name());
            assertEquals(reason.label(), reason.getLabel());
            assertEquals(reason.name(), reason.getCode());
        }
        for (RemakeReason reason : RemakeReason.values()) {
            assertTrue(reason.label() != null && !reason.label().isBlank(), reason.name());
            assertEquals(reason.label(), reason.getLabel());
            assertEquals(reason.name(), reason.getCode());
        }
    }

    private static List<String> describe(IssueReason[] values) {
        List<String> out = new ArrayList<>();
        for (IssueReason reason : values) out.add(reason.name() + "=" + reason.label());
        return out;
    }

    private static List<String> describe(RemakeReason[] values) {
        List<String> out = new ArrayList<>();
        for (RemakeReason reason : values) out.add(reason.name() + "=" + reason.label());
        return out;
    }
}
