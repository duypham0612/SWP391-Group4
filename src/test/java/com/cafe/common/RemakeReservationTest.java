package com.cafe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ★ Bất biến kho của luồng làm lại: tổng lượng ĐÃ GHI SỔ (DEDUCT + WASTE) phải luôn bằng số lượt
 * pha đã thực sự dùng nguyên liệu, cộng phần giữ chỗ cho lượt kế tiếp nếu có.
 *
 * Sổ được mô phỏng theo đơn vị "lượt pha" thay vì gam/ml — định mức mỗi lượt là như nhau nên
 * quy tắc giữ chỗ chỉ cần đếm lượt là đủ để lộ chỗ trừ thiếu/trừ thừa.
 */
class RemakeReservationTest {

    /** Sổ mô phỏng của MỘT dòng món đi qua các lần bấm Xong / Làm lại trên màn quầy pha chế. */
    private static final class Ledger {
        int booked;         // tổng số lượt pha đã ghi sổ (DEDUCT + WASTE)
        int poured;         // số lượt pha thực tế đã dùng nguyên liệu
        boolean reserved;   // cờ RemakeInventoryReserved hiện tại
        boolean ready;      // món đang ở READY (đã bấm Xong)

        /** Bấm Xong: hoàn thành một lượt pha; trừ kho trừ khi lượt này đã được giữ chỗ sẵn. */
        void markReady() {
            poured++;
            if (!reserved) booked++;
            reserved = false;   // completeClaimed luôn reset cờ
            ready = true;
        }

        /** Bấm Làm lại: luôn ghi hao hụt đúng MỘT lượt pha (reserveRemakeForOrderItem). */
        void remake() {
            if (!ready) poured++;   // bỏ từ MAKING: lượt đang pha cũng đã dùng nguyên liệu rồi
            booked++;
            reserved = RemakeReservation.reservesNextPour(ready, reserved);
            ready = false;
        }

        /**
         * Huỷ món (cancelItem/voidOrder): lượt đang được giữ chỗ sẽ không bao giờ pha nữa nên dòng
         * WASTE giữ chỗ phải hoàn về kho — releaseRemakeReservation làm việc này.
         */
        void cancel() {
            if (reserved) booked--;
            reserved = false;
            ready = false;
        }
    }

    /** Sổ phải khớp thực tế ở MỌI thời điểm: đã ghi = đã pha + (giữ chỗ cho lượt tới ? 1 : 0). */
    private static void assertBalanced(Ledger l, String at) {
        assertEquals(l.poured + (l.reserved ? 1 : 0), l.booked,
                "Lệch sổ kho tại: " + at + " (đã ghi " + l.booked + ", đã pha " + l.poured
                        + ", giữ chỗ " + l.reserved + ")");
    }

    /** ★ Test hồi quy: làm lại từ MAKING — trước đây luôn giữ chỗ nên trừ THIẾU đúng một lượt. */
    @Test
    void remakeFromMaking_booksTheDiscardedPour_notTheNextOne() {
        Ledger l = new Ledger();
        l.remake();                       // pha dở rồi làm đổ → bỏ, ghi hao hụt lượt vừa bỏ
        assertFalse(l.reserved, "Lượt vừa bỏ chưa từng được trừ kho → KHÔNG được giữ chỗ");
        assertBalanced(l, "sau khi làm lại từ MAKING");

        l.markReady();                    // pha lại rồi bấm Xong → phải trừ kho bình thường
        assertBalanced(l, "sau khi pha lại và bấm Xong");
        assertEquals(2, l.booked, "Hai lượt pha thật phải được ghi sổ đủ hai lượt");
    }

    /** Làm lại từ READY: lượt bỏ đã có DEDUCT nên dòng WASTE giữ chỗ cho lượt kế tiếp. */
    @Test
    void remakeFromReady_reservesTheNextPour() {
        Ledger l = new Ledger();
        l.markReady();                    // lượt 1 đã trừ kho
        assertBalanced(l, "sau khi bấm Xong lần đầu");

        l.remake();                       // khách phản hồi → bỏ ly đã pha
        assertTrue(l.reserved, "Lượt bỏ đã được trừ kho → dòng WASTE giữ chỗ cho lượt kế tiếp");
        assertBalanced(l, "sau khi làm lại từ READY");

        l.markReady();                    // pha lại: KHÔNG trừ lần hai
        assertBalanced(l, "sau khi pha lại và bấm Xong");
        assertEquals(2, l.booked, "Hai lượt pha thật — không trừ thừa lượt nào");
    }

    /** Làm lại từ MAKING nhưng lượt đó đã được giữ chỗ từ lần làm lại trước → vẫn giữ chỗ tiếp. */
    @Test
    void remakeFromMaking_whenAlreadyReserved_keepsReservation() {
        Ledger l = new Ledger();
        l.markReady();                    // lượt 1: DEDUCT
        l.remake();                       // bỏ từ READY → giữ chỗ lượt 2
        l.remake();                       // đang pha lượt 2 lại làm đổ → bỏ từ MAKING
        assertTrue(l.reserved, "Lượt vừa bỏ đã được giữ chỗ sẵn → dòng WASTE mới giữ chỗ lượt kế tiếp");
        assertBalanced(l, "sau hai lần làm lại liên tiếp");

        l.markReady();
        assertBalanced(l, "sau khi pha lại và bấm Xong");
        assertEquals(3, l.booked, "Ba lượt pha thật phải được ghi đủ ba lượt");
    }

    /** Làm lại nhiều lần liên tiếp ngay từ MAKING — mỗi lượt bỏ đều được ghi đúng một lần. */
    @Test
    void repeatedRemakeFromMaking_staysBalanced() {
        Ledger l = new Ledger();
        for (int i = 0; i < 4; i++) {
            l.remake();
            assertFalse(l.reserved, "Chưa lượt nào được trừ trước đó → không giữ chỗ");
            assertBalanced(l, "sau lần làm lại thứ " + (i + 1));
        }
        l.markReady();
        assertBalanced(l, "sau khi bấm Xong");
        assertEquals(5, l.booked, "4 lượt bỏ + 1 lượt giao khách = 5 lượt ghi sổ");
    }

    /** ★ Test hồi quy: huỷ món khi đang giữ chỗ — trước đây phần giữ chỗ bị bỏ quên, kho trừ THỪA một lượt. */
    @Test
    void cancelWhileReserved_returnsTheReservedPour() {
        Ledger l = new Ledger();
        l.markReady();                    // lượt 1 đã trừ kho
        l.remake();                       // khách phản hồi → bỏ ly, dòng WASTE giữ chỗ lượt 2
        assertTrue(l.reserved, "Sau khi làm lại từ READY phải đang giữ chỗ");

        l.cancel();                       // khách đổi ý, Thu ngân huỷ món → lượt 2 không bao giờ pha
        assertFalse(l.reserved, "Huỷ món thì không còn giữ chỗ nào");
        assertBalanced(l, "sau khi huỷ món đang giữ chỗ");
        assertEquals(1, l.booked, "Chỉ một ly được pha thật nên sổ chỉ được ghi một lượt");
    }

    /** Huỷ món sau khi làm lại từ MAKING: không có gì để hoàn, sổ vẫn phải khớp. */
    @Test
    void cancelWithoutReservation_leavesLedgerUntouched() {
        Ledger l = new Ledger();
        l.remake();                       // bỏ từ MAKING → WASTE là sổ của lượt vừa bỏ, không giữ chỗ
        assertFalse(l.reserved, "Bỏ từ MAKING khi chưa trừ gì thì không giữ chỗ");

        l.cancel();
        assertBalanced(l, "sau khi huỷ món không giữ chỗ");
        assertEquals(1, l.booked, "Lượt pha dở bị bỏ vẫn phải nằm trong sổ");
    }

    /** Huỷ đơn ngay sau hai lần làm lại liên tiếp — chỉ hoàn đúng lượt giữ chỗ cuối cùng. */
    @Test
    void cancelAfterRepeatedRemakes_returnsOnlyTheLastReservation() {
        Ledger l = new Ledger();
        l.markReady();                    // lượt 1: DEDUCT
        l.remake();                       // bỏ từ READY → giữ chỗ lượt 2
        l.remake();                       // pha lượt 2 lại hỏng → bỏ từ MAKING, vẫn giữ chỗ lượt 3
        assertEquals(3, l.booked, "Hai lượt đã pha + một lượt giữ chỗ");

        l.cancel();                       // huỷ món: chỉ lượt giữ chỗ thứ ba được hoàn
        assertBalanced(l, "sau khi huỷ món qua hai lần làm lại");
        assertEquals(2, l.booked, "Hai lượt đã pha thật vẫn phải giữ nguyên trong sổ");
    }

    /** Bảng quyết định thuần — chốt lại đúng bốn tổ hợp đầu vào. */
    @Test
    void decisionTable() {
        assertTrue(RemakeReservation.reservesNextPour(true, false),  "READY, chưa giữ chỗ → giữ chỗ");
        assertTrue(RemakeReservation.reservesNextPour(true, true),   "READY, đã giữ chỗ → giữ chỗ");
        assertTrue(RemakeReservation.reservesNextPour(false, true),  "MAKING, đã giữ chỗ → giữ chỗ");
        assertFalse(RemakeReservation.reservesNextPour(false, false), "MAKING, chưa giữ chỗ → KHÔNG giữ chỗ");
    }
}
