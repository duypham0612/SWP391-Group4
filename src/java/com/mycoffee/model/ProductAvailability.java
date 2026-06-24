package com.mycoffee.model;

/**
 * DTO hiển thị sản phẩm + trạng thái còn bán (cho màn Quản lý sản phẩm / Báo hết món).
 */
public class ProductAvailability {
    private int productId;
    private String productName;
    private double basePrice;
    private String categoryName;
    private boolean available;

    public ProductAvailability() {
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
