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
    private boolean showOnHome = true;
    private int homeSortOrder = 0;
    private int prepSeconds = 720;
    private boolean sizeEnabled = true;
    private BigDecimal sizeSDelta = BigDecimal.ZERO;
    private BigDecimal sizeMDelta = BigDecimal.ZERO;
    private BigDecimal sizeLDelta = BigDecimal.ZERO;

    private String categoryName;

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice; }

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

    public int getPrepMinutes() { return prepSeconds / 60; }

    public boolean isSizeEnabled() { return sizeEnabled; }
    public void setSizeEnabled(boolean sizeEnabled) { this.sizeEnabled = sizeEnabled; }

    public BigDecimal getSizeSDelta() { return sizeSDelta; }
    public void setSizeSDelta(BigDecimal sizeSDelta) { this.sizeSDelta = sizeSDelta == null ? BigDecimal.ZERO : sizeSDelta; }

    public BigDecimal getSizeMDelta() { return sizeMDelta; }
    public void setSizeMDelta(BigDecimal sizeMDelta) { this.sizeMDelta = sizeMDelta == null ? BigDecimal.ZERO : sizeMDelta; }

    public BigDecimal getSizeLDelta() { return sizeLDelta; }
    public void setSizeLDelta(BigDecimal sizeLDelta) { this.sizeLDelta = sizeLDelta == null ? BigDecimal.ZERO : sizeLDelta; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
