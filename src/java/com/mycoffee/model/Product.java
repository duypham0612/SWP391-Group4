package com.mycoffee.model;

public class Product {
    private int productId;
    private String productName;
    private int categoryId;
    private double basePrice;
    private String imageUrl;
    private String description;
    private boolean isAvailable;

    // Constructors
    public Product() {
    }

    public Product(int productId, String productName, int categoryId, double basePrice, String imageUrl, String description, boolean isAvailable) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.imageUrl = imageUrl;
        this.description = description;
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    // Đổi tên getter/setter về một chuẩn duy nhất categoryId
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ĐÃ SỬA: Đổi tên chuẩn từ isIsAvailable() thành isAvailable() để JSP nhận diện được ${p.isAvailable}
    public boolean isAvailable() { return isAvailable; }
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }
}
