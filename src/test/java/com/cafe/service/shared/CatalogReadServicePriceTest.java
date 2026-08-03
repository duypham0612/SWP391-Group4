package com.cafe.service.shared;

import com.cafe.model.BranchMenuItem;
import com.cafe.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogReadServicePriceTest {

    @Test
    void publicMenuPrefersBranchPriceAndFallsBackToBasePrice() {
        Product product = new Product();
        product.setBasePrice(new BigDecimal("29000"));
        BranchMenuItem branchItem = new BranchMenuItem();

        assertEquals(new BigDecimal("29000"),
                CatalogReadService.effectivePublicPrice(product, branchItem));

        branchItem.setLocalPrice(new BigDecimal("23200"));
        assertEquals(new BigDecimal("23200"),
                CatalogReadService.effectivePublicPrice(product, branchItem));
    }

    @Test
    void publicMenuOnlyShowsPublishedAndListedBranchItems() {
        assertFalse(CatalogReadService.isPubliclyListed(null));

        BranchMenuItem branchItem = new BranchMenuItem();
        assertFalse(CatalogReadService.isPubliclyListed(branchItem));

        branchItem.setPublished(true);
        assertTrue(CatalogReadService.isPubliclyListed(branchItem));

        branchItem.setListed(false);
        assertFalse(CatalogReadService.isPubliclyListed(branchItem));

        branchItem.setListed(true);
        branchItem.setPublished(false);
        assertFalse(CatalogReadService.isPubliclyListed(branchItem));
    }
}
