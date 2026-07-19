package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ★ Test cho validate form "Báo tạm hết món" — chặn barista thao tác tuỳ ý. */
class Menu86ValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 19, 10, 0);

    /** Thời gian hợp lệ mặc định để test riêng nhánh lý do / ghi chú. */
    private static final LocalDateTime OK_ETA = NOW.plusHours(2);

    private static String msg(Executable call) {
        return assertThrows(BusinessException.class, call::run).getMessage();
    }

    private interface Executable { void run(); }

    private static String repeat(String s, int times) { return s.repeat(times); }

    // ---------- Lý do ----------

    @Test
    void reason_null_is_rejected() {
        assertTrue(msg(() -> Menu86Validator.validate(null, null, OK_ETA, NOW)).contains("lý do"));
    }

    @Test
    void reason_blank_is_rejected() {
        assertTrue(msg(() -> Menu86Validator.validate("   ", null, OK_ETA, NOW)).contains("lý do"));
    }

    @Test
    void reason_unknown_code_is_rejected() {
        // Client tự chế mã → không tin, chặn ở server
        assertTrue(msg(() -> Menu86Validator.validate("FREE_TEXT_HACK", null, OK_ETA, NOW)).contains("lý do"));
    }

    @Test
    void reason_accepts_lowercase_and_padding() {
        Menu86Validator.Validated v =
                Menu86Validator.validate("  equipment  ", null, OK_ETA, NOW);
        assertSame(Reason86.EQUIPMENT, v.getReason());
    }

    @Test
    void every_reason_code_is_accepted() {
        for (Reason86 r : Reason86.values()) {
            String note = r == Reason86.OTHER ? "Ghi chú đủ dài cho lý do khác" : null;
            assertSame(r, Menu86Validator.validate(r.name(), note, OK_ETA, NOW).getReason());
        }
    }

    // ---------- Ghi chú ----------

    @Test
    void other_requires_note() {
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", null, OK_ETA, NOW)).contains("ghi rõ"));
    }

    @Test
    void other_rejects_blank_note() {
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", "      ", OK_ETA, NOW)).contains("ghi rõ"));
    }

    @Test
    void other_rejects_note_shorter_than_min() {
        // 9 ký tự — thiếu 1
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", "123456789", OK_ETA, NOW)).contains("ghi rõ"));
    }

    @Test
    void other_accepts_note_at_exactly_min_length() {
        // Biên: đúng 10 ký tự → cho qua
        Menu86Validator.Validated v = Menu86Validator.validate("OTHER", "1234567890", OK_ETA, NOW);
        assertEquals("1234567890", v.getNote());
    }

    @Test
    void other_note_length_counted_after_trim() {
        // 9 ký tự thật + khoảng trắng đệm cho "trông dài" → vẫn phải trượt
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", "   123456789   ", OK_ETA, NOW))
                .contains("ghi rõ"));
    }

    @Test
    void other_accepts_vietnamese_note_at_min_length() {
        // "Máy hư rồi" = 10 ký tự người dùng nhìn thấy (có dấu) → không được tính lố
        Menu86Validator.Validated v = Menu86Validator.validate("OTHER", "Máy hư rồi", OK_ETA, NOW);
        assertEquals("Máy hư rồi", v.getNote());
    }

    @Test
    void non_other_reason_allows_empty_note() {
        Menu86Validator.Validated v = Menu86Validator.validate("INGREDIENT_OUT", null, OK_ETA, NOW);
        assertNull(v.getNote());
    }

    @Test
    void blank_note_is_normalized_to_null() {
        Menu86Validator.Validated v = Menu86Validator.validate("INGREDIENT_OUT", "   ", OK_ETA, NOW);
        assertNull(v.getNote());
    }

    @Test
    void note_is_trimmed() {
        Menu86Validator.Validated v =
                Menu86Validator.validate("INGREDIENT_OUT", "  Hết sữa tươi  ", OK_ETA, NOW);
        assertEquals("Hết sữa tươi", v.getNote());
    }

    @Test
    void chip_joined_note_passes_through() {
        // Bấm nhiều chip → nối chuỗi, lưu text thuần
        String joined = "Hết sữa tươi · Hết đá";
        Menu86Validator.Validated v = Menu86Validator.validate("INGREDIENT_OUT", joined, OK_ETA, NOW);
        assertEquals(joined, v.getNote());
    }

    @Test
    void note_at_max_length_is_accepted() {
        String note = repeat("a", 255);
        assertEquals(note, Menu86Validator.validate("INGREDIENT_OUT", note, OK_ETA, NOW).getNote());
    }

    @Test
    void note_over_max_length_is_rejected() {
        // 256 ký tự → tràn NVARCHAR(255), phải chặn trước khi xuống DAO
        String note = repeat("a", 256);
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", note, OK_ETA, NOW))
                .contains("tối đa"));
    }

    @Test
    void long_vietnamese_note_over_max_is_rejected() {
        String note = repeat("á", 256);
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", note, OK_ETA, NOW))
                .contains("tối đa"));
    }

    // ---------- Chuẩn hoá Unicode (bàn phím macOS gõ dạng tổ hợp) ----------

    private static String nfd(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
    }

    @Test
    void decomposed_vietnamese_is_normalized_before_counting() {
        // Gõ trên macOS: "Máy hư rồi" ra dạng tổ hợp = 13 code point, nhưng người dùng thấy 10 ký tự
        String typed = nfd("Máy hư rồi");
        assertTrue(typed.length() > 10, "chuỗi mẫu phải ở dạng tổ hợp");
        Menu86Validator.Validated v = Menu86Validator.validate("OTHER", typed, OK_ETA, NOW);
        assertEquals("Máy hư rồi", v.getNote());   // lưu xuống DB ở dạng dựng sẵn
    }

    @Test
    void decomposed_note_below_min_is_still_rejected() {
        // 9 ký tự nhìn thấy, dạng tổ hợp đếm thô ra 12 → không được lọt
        String typed = nfd("Máy hư rồ");
        assertTrue(typed.length() >= 10, "chuỗi mẫu phải dài hơn 10 nếu đếm thô");
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", typed, OK_ETA, NOW)).contains("ghi rõ"));
    }

    @Test
    void decomposed_long_note_is_measured_after_normalizing() {
        // 200 ký tự dựng sẵn (vừa cột) nhưng gõ dạng tổ hợp thành 400 đơn vị → chuẩn hoá xong phải pass
        String typed = nfd(repeat("á", 200));
        assertTrue(typed.length() > Constants.MENU86_NOTE_MAX_CHARS);
        assertEquals(200, Menu86Validator.validate("INGREDIENT_OUT", typed, OK_ETA, NOW).getNote().length());
    }

    @Test
    void emoji_counts_as_two_units_against_column_limit() {
        // Emoji = 2 đơn vị UTF-16 = 2 chỗ trong NVARCHAR(255). 128 emoji = 256 → tràn cột, phải chặn.
        String emojis = repeat("😀", 128);
        assertEquals(256, emojis.length());
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", emojis, OK_ETA, NOW))
                .contains("tối đa"));
    }

    @Test
    void emoji_within_column_limit_is_accepted() {
        String emojis = repeat("😀", 127);   // 254 đơn vị → vừa cột
        assertEquals(254, emojis.length());
        assertEquals(emojis, Menu86Validator.validate("INGREDIENT_OUT", emojis, OK_ETA, NOW).getNote());
    }

    @Test
    void ten_emoji_satisfy_min_visible_length() {
        // 10 emoji = 10 ký tự người dùng thấy (dù 20 đơn vị UTF-16) → đủ mức tối thiểu
        String emojis = repeat("😀", 10);
        assertNotNull(Menu86Validator.validate("OTHER", emojis, OK_ETA, NOW).getNote());
    }

    // ---------- Dự kiến có lại ----------

    @Test
    void eta_null_is_rejected() {
        // Đây là lỗ hiện tại: ETA để trống vẫn báo hết được
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", null, null, NOW))
                .contains("dự kiến có lại"));
    }

    @Test
    void eta_in_the_past_is_rejected() {
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", null, NOW.minusMinutes(1), NOW))
                .contains("tương lai"));
    }

    @Test
    void eta_equal_now_is_rejected() {
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", null, NOW, NOW))
                .contains("tương lai"));
    }

    @Test
    void eta_below_min_gap_is_rejected() {
        // 14 phút 59 giây → thiếu 1 giây so với mốc 15 phút
        LocalDateTime eta = NOW.plusMinutes(14).plusSeconds(59);
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", null, eta, NOW))
                .contains("ít nhất"));
    }

    @Test
    void eta_at_exactly_min_gap_is_accepted() {
        // Biên: đúng 15 phút → cho qua
        LocalDateTime eta = NOW.plusMinutes(15);
        assertEquals(eta, Menu86Validator.validate("INGREDIENT_OUT", null, eta, NOW).getBackInEta());
    }

    @Test
    void eta_at_exactly_max_horizon_is_accepted() {
        // Biên: đúng 7 ngày → cho qua
        LocalDateTime eta = NOW.plusDays(7);
        assertEquals(eta, Menu86Validator.validate("INGREDIENT_OUT", null, eta, NOW).getBackInEta());
    }

    @Test
    void eta_beyond_max_horizon_is_rejected() {
        LocalDateTime eta = NOW.plusDays(7).plusSeconds(1);
        assertTrue(msg(() -> Menu86Validator.validate("INGREDIENT_OUT", null, eta, NOW))
                .contains("quá"));
    }

    // ---------- Thứ tự lỗi & happy path ----------

    @Test
    void reason_error_wins_when_everything_is_invalid() {
        // Sai cả ba → thông báo phải ổn định, không phụ thuộc thứ tự trường trên form
        assertTrue(msg(() -> Menu86Validator.validate(null, null, NOW.minusDays(1), NOW))
                .contains("lý do"));
    }

    @Test
    void note_error_wins_over_eta_error() {
        assertTrue(msg(() -> Menu86Validator.validate("OTHER", "ngắn", NOW.minusDays(1), NOW))
                .contains("ghi rõ"));
    }

    @Test
    void valid_form_returns_all_fields() {
        LocalDateTime eta = NOW.plusHours(3);
        Menu86Validator.Validated v =
                Menu86Validator.validate("EQUIPMENT", "Máy pha lỗi", eta, NOW);
        assertNotNull(v);
        assertSame(Reason86.EQUIPMENT, v.getReason());
        assertEquals("Máy pha lỗi", v.getNote());
        assertEquals(eta, v.getBackInEta());
    }

    // ---------- Chip bấm nhanh ----------

    @Test
    void other_reason_has_no_quick_notes() {
        assertTrue(Reason86.OTHER.quickNotes().isEmpty());
    }

    @Test
    void non_other_reasons_have_quick_notes_and_label() {
        for (Reason86 r : Reason86.values()) {
            assertTrue(r.label() != null && !r.label().isBlank(), "thiếu nhãn: " + r);
            if (r != Reason86.OTHER) {
                assertTrue(!r.quickNotes().isEmpty(), "thiếu chip: " + r);
            }
        }
    }

    @Test
    void quick_notes_fit_within_note_limit() {
        // Bấm hết chip của một lý do vẫn không được tràn 255 ký tự
        for (Reason86 r : Reason86.values()) {
            String joined = String.join(" · ", r.quickNotes());
            assertTrue(joined.length() <= Constants.MENU86_NOTE_MAX_CHARS,
                    "chip nối chuỗi vượt giới hạn: " + r);
        }
    }
}
