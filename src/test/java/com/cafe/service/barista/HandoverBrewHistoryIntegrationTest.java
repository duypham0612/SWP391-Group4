package com.cafe.service.barista;

import com.cafe.model.OrderItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration test cần SQL Server có dữ liệu OrderItem; bật khi chạy môi trường DB local. */
class HandoverBrewHistoryIntegrationTest {

    private static final int BRANCH_ID = 1;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    @Disabled("Cần SQL Server CafeChain local với dữ liệu READY/PICKED_UP/SERVED để xác nhận truy vấn bàn giao")
    void getBrewHistory_filtersByVietnamDayAndBranch() throws Exception {
        List<OrderItem> history = new HandoverService().getBrewHistory(BRANCH_ID);
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDateTime fromUtc = today.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime toUtc = today.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        for (OrderItem item : history) {
            assertNotNull(item.getDoneAt());
            assertTrue(item.getDoneAt().compareTo(fromUtc) >= 0);
            assertTrue(item.getDoneAt().compareTo(toUtc) < 0);
            assertTrue(BRANCH_ID == item.getOrderBranchId());
            assertTrue("READY".equals(item.getStatus()) || "PICKED_UP".equals(item.getStatus()) || "SERVED".equals(item.getStatus()));
        }
    }
}
