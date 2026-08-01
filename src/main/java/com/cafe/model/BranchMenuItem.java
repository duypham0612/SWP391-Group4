package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dòng menu theo chi nhánh = Product + trạng thái BranchMenu (có thể chưa publish). */
public class BranchMenuItem {
    private int branchId;
    private int productId;
    private String productName;
    private BigDecimal basePrice;
    private boolean published;        // đã có dòng catalog.BranchMenu chưa
    private boolean listed = true;
    private BigDecimal localPrice;    // NULL = dùng BasePrice
    private boolean temporarilyUnavailable;
    private LocalDateTime backInEta;  // B3.F3 — dự kiến có lại (NULL = chưa rõ)
    private String imageUrl;          // ảnh sản phẩm (catalog.Product.ImageUrl)

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

    public boolean isListed() { return listed; }
    public void setListed(boolean listed) { this.listed = listed; }

    public BigDecimal getLocalPrice() { return localPrice; }
    public void setLocalPrice(BigDecimal localPrice) { this.localPrice = localPrice; }
    public boolean isTemporarilyUnavailable() { return temporarilyUnavailable; }
    public void setTemporarilyUnavailable(boolean v) { this.temporarilyUnavailable = v; }

    public LocalDateTime getBackInEta() { return backInEta; }
    public void setBackInEta(LocalDateTime backInEta) { this.backInEta = backInEta; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

}
