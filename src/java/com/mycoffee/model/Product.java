package com.mycoffee.model;

public class Product {
    private int productId;
    private String productName;
    private double basePrice;
    private int categoryId;
    private String imageUrl;    // Thuộc tính lưu đường dẫn hình ảnh
    private String description; // Thuộc tính mô tả món ăn

    public Product() {}

    // Constructor cũ 4 tham số (giữ lại để tránh lỗi các vùng code khác chưa cập nhật)
    public Product(int productId, String productName, double basePrice, int categoryId) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
    }

    // Constructor 5 tham số bao gồm cả hình ảnh
    public Product(int productId, String productName, double basePrice, int categoryId, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
    }

    // Constructor mới đầy đủ 6 tham số bao gồm cả hình ảnh và mô tả
    public Product(int productId, String productName, double basePrice, int categoryId, String imageUrl, String description) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
        this.imageUrl = imageUrl;
        this.description = description;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    // GETTER & SETTER CHO TRƯỜNG IMAGEURL
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // GETTER & SETTER CHO TRƯỜNG DESCRIPTION
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}