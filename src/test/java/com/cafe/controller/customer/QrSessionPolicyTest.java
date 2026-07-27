package com.cafe.controller.customer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QrSessionPolicyTest {

    @Test
    void uses_bound_session_when_request_omits_id() {
        assertEquals(41, QrSessionPolicy.resolve(41, null));
        assertEquals(41, QrSessionPolicy.resolve(41, ""));
        assertEquals(41, QrSessionPolicy.resolve(41, "  "));
    }

    @Test
    void accepts_id_matching_the_scanned_table_session() {
        assertEquals(41, QrSessionPolicy.resolve(41, "41"));
        assertEquals(41, QrSessionPolicy.resolve(41, " 41 "));
    }

    /** Bàn khác = phiên khác: khách bàn 41 không được xem/huỷ đơn phiên 42. */
    @Test
    void rejects_id_of_another_table_session() {
        assertNull(QrSessionPolicy.resolve(41, "42"));
        assertNull(QrSessionPolicy.resolve(41, "0"));
        assertNull(QrSessionPolicy.resolve(41, "-41"));
    }

    /** Chưa quét QR thì không có phiên nào để thao tác — kể cả khi tự soạn URL đúng id. */
    @Test
    void rejects_when_no_qr_scan_bound_the_session() {
        assertNull(QrSessionPolicy.resolve(null, "41"));
        assertNull(QrSessionPolicy.resolve(null, null));
        assertNull(QrSessionPolicy.resolve("41", "41"));   // attribute sai kiểu → không tin
    }

    @Test
    void rejects_non_numeric_id() {
        assertNull(QrSessionPolicy.resolve(41, "41abc"));
        assertNull(QrSessionPolicy.resolve(41, "'; DROP TABLE"));
    }
}
