package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Branch menu row: Product plus branch-level selling state. */
public class BranchMenuItem {
    private int branchId;
    private int productId;
    private String productName;
    private BigDecimal basePrice;
    private boolean published;
    private boolean available = true;
    private BigDecimal localPrice;
    private boolean is86;
    private LocalDateTime backInEta;
    private String imageUrl;
    private boolean sizeEnabled = true;
    private BigDecimal sizeSDelta = BigDecimal.ZERO;
    private BigDecimal sizeMDelta = BigDecimal.ZERO;
    private BigDecimal sizeLDelta = BigDecimal.ZERO;

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

    public LocalDateTime getBackInEta() { return backInEta; }
    public void setBackInEta(LocalDateTime backInEta) { this.backInEta = backInEta; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isSizeEnabled() { return sizeEnabled; }
    public void setSizeEnabled(boolean sizeEnabled) { this.sizeEnabled = sizeEnabled; }

    public BigDecimal getSizeSDelta() { return sizeSDelta; }
    public void setSizeSDelta(BigDecimal sizeSDelta) { this.sizeSDelta = sizeSDelta == null ? BigDecimal.ZERO : sizeSDelta; }

    public BigDecimal getSizeMDelta() { return sizeMDelta; }
    public void setSizeMDelta(BigDecimal sizeMDelta) { this.sizeMDelta = sizeMDelta == null ? BigDecimal.ZERO : sizeMDelta; }

    public BigDecimal getSizeLDelta() { return sizeLDelta; }
    public void setSizeLDelta(BigDecimal sizeLDelta) { this.sizeLDelta = sizeLDelta == null ? BigDecimal.ZERO : sizeLDelta; }

    public String getBackInEtaText() {
        return backInEta == null ? "" :
                backInEta.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }
}
