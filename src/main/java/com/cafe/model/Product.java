package com.cafe.model;

import java.math.BigDecimal;

/** catalog.Product */
public class Product {
    private int productId;
    private int categoryId;
    private String name;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private String imageUrl;
    private boolean active = true;
    private boolean showOnHome = true;   // hiển thị trên trang Home công khai
    private int homeSortOrder = 0;        // thứ tự ưu tiên trong danh mục trên Home (nhỏ = trước)
    private int prepSeconds = 720;        // thời gian pha chuẩn (giây); nền cho SLA theo món (mặc định 12 phút)

    private String categoryName; // join để hiển thị

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isShowOnHome() { return showOnHome; }
    public void setShowOnHome(boolean showOnHome) { this.showOnHome = showOnHome; }

    public int getHomeSortOrder() { return homeSortOrder; }
    public void setHomeSortOrder(int homeSortOrder) { this.homeSortOrder = homeSortOrder; }

    public int getPrepSeconds() { return prepSeconds; }
    public void setPrepSeconds(int prepSeconds) { this.prepSeconds = prepSeconds; }

    /** Phút (làm tròn) để hiển thị/nhập trên màn admin — EL `div` trả số thực nên tính sẵn ở đây. */
    public int getPrepMinutes() { return prepSeconds / 60; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
