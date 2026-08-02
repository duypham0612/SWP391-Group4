package com.cafe.service.barista;

import com.cafe.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chế độ cao điểm: khi hàng chờ dồn quá ngưỡng, bảng chuyển từ CẢNH BÁO sang XẾP THỨ TỰ.
 * Logic thuần, không đụng DB.
 */
class KdsPeakTest {

    /** Dưới ngưỡng thì bình thường; chạm ngưỡng thì bật cao điểm. */
    @Test
    void peak_turns_on_at_threshold() {
        assertFalse(KdsService.isPeak(11, 12));
        assertTrue(KdsService.isPeak(12, 12));
        assertTrue(KdsService.isPeak(20, 12));
    }

    /** Ngưỡng chi nhánh = 0 nghĩa là dùng mặc định toàn hệ, không phải "cao điểm ngay". */
    @Test
    void zero_branch_threshold_falls_back_to_global_default() {
        assertFalse(KdsService.isPeak(Constants.PEAK_THRESHOLD_CUPS - 1, 0));
        assertTrue(KdsService.isPeak(Constants.PEAK_THRESHOLD_CUPS, 0));
    }

    /** Chi nhánh đặt ngưỡng riêng thì thắng mặc định. */
    @Test
    void branch_threshold_overrides_default() {
        assertTrue(KdsService.isPeak(6, 5));    // ngưỡng riêng 5, thấp hơn mặc định
        assertFalse(KdsService.isPeak(6, 8));   // ngưỡng riêng 8, cao hơn
    }
}
