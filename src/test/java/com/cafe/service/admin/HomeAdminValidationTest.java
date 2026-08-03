package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.model.Branch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HomeAdminValidationTest {

    @Test
    void valid_content_is_trimmed_and_accepts_https_or_internal_asset() {
        Branch branch = content("  Trang Home Nguyễn Huệ  ", "https://images.example.com/hero.jpg");

        HomeAdminService.validateAndNormalizeContent(branch);

        assertEquals("Trang Home Nguyễn Huệ", branch.getHeroTitle());
        branch.setHeroImageUrl(" /assets/img/login-hero.svg ");
        HomeAdminService.validateAndNormalizeContent(branch);
        assertEquals("/assets/img/login-hero.svg", branch.getHeroImageUrl());
    }

    @Test
    void content_rejects_missing_title_oversize_text_and_unsafe_image_url() {
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateAndNormalizeContent(content("   ", null)));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateAndNormalizeContent(content("x".repeat(201), null)));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateAndNormalizeContent(content("Trang Home", "javascript:alert(1)")));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateAndNormalizeContent(content("Trang Home", "/assets/../secret.txt")));
    }

    @Test
    void product_batch_rejects_empty_duplicate_mismatched_and_invalid_order_data() {
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateHomeProductBatch(null, null, null));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateHomeProductBatch(
                        new int[]{1, 1}, new boolean[]{true, false}, new int[]{0, 1}));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateHomeProductBatch(
                        new int[]{1}, new boolean[]{true, false}, new int[]{0}));
        assertThrows(BusinessException.class,
                () -> HomeAdminService.validateHomeProductBatch(
                        new int[]{1}, new boolean[]{true}, new int[]{-1}));
    }

    @Test
    void product_batch_accepts_multiple_distinct_products() {
        HomeAdminService.validateHomeProductBatch(
                new int[]{1, 2, 3}, new boolean[]{true, false, true}, new int[]{0, 10, 20});
    }

    private Branch content(String title, String imageUrl) {
        Branch branch = new Branch();
        branch.setBranchId(1);
        branch.setHeroTitle(title);
        branch.setHeroImageUrl(imageUrl);
        return branch;
    }
}
