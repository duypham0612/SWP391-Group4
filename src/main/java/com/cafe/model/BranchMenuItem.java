package com.cafe.model;

import java.math.BigDecimal;

/** Dòng menu theo chi nhánh = Product + trạng thái BranchMenu (có thể chưa publish). */
public class BranchMenuItem {
    private int branchId;
    private int productId;
    private String productName;
    private BigDecimal basePrice;
    private boolean published;        // đã có dòng catalog.BranchMenu chưa
    private boolean available = true;
    private BigDecimal localPrice;    // NULL = dùng BasePrice
    private boolean is86;

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public BigDecimal getLocalPrice() { return localPrice; }
    public void setLocalPrice(BigDecimal localPrice) { this.localPrice = localPrice; }

    public boolean isIs86() { return is86; }
    public void setIs86(boolean is86) { this.is86 = is86; }
}
