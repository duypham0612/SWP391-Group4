package com.cafe.web.viewmodel;

import com.cafe.model.PosMenuItem;
import com.cafe.model.ProductStockStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewFormatterStockTest {
    private final ViewFormatter formatter = new ViewFormatter();

    @Test
    void formats_low_and_out_stock_from_raw_domain_data() {
        PosMenuItem low = new PosMenuItem();
        low.setAvailabilityState(ProductStockStatus.LOW);
        low.setLowIngredients(Set.of("Syrup Đào"));
        assertEquals("Sắp hết Syrup Đào", formatter.stockMessage(low));

        PosMenuItem out = new PosMenuItem();
        out.setAvailabilityState(ProductStockStatus.OUT);
        out.setOutIngredients(Set.of("Đá", "Sữa"));
        String message = formatter.stockMessage(out);
        // Set không cam kết thứ tự; nội dung hiển thị phải đủ prefix và tên nguyên liệu.
        org.junit.jupiter.api.Assertions.assertTrue(message.startsWith("Hết "));
        org.junit.jupiter.api.Assertions.assertTrue(message.contains("Đá"));
        org.junit.jupiter.api.Assertions.assertTrue(message.contains("Sữa"));
    }

    @Test
    void formats_manual_menu_block_without_domain_label() {
        PosMenuItem item = new PosMenuItem();
        item.setAvailabilityState("EIGHTY_SIX");
        assertEquals("Tạm ngừng bán", formatter.stockMessage(item));
    }
}
