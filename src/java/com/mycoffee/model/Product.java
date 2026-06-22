package com.mycoffee.model;

public class Product {
    private int productId;
    private String productName;
    private double basePrice;
    private int categoryId;
    private String categoryName;
    private String imageUrl;
    private String description;

    public Product() {}

    public Product(int productId, String productName, double basePrice, int categoryId) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
    }

    public Product(int productId, String productName, double basePrice, int categoryId,
                   String categoryName, String imageUrl, String description) {
        this.productId = productId;
        this.productName = productName;
        this.basePrice = basePrice;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
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
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
