package com.cafe.model;

import java.math.BigDecimal;

/** One item on the POS/QR menu. */
public class PosMenuItem {
    private int productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private boolean sizeEnabled = true;
    private BigDecimal sizeSDelta = BigDecimal.ZERO;
    private BigDecimal sizeMDelta = BigDecimal.ZERO;
    private BigDecimal sizeLDelta = BigDecimal.ZERO;

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal v) { this.price = v; }

    public boolean isSizeEnabled() { return sizeEnabled; }
    public void setSizeEnabled(boolean v) { this.sizeEnabled = v; }

    public BigDecimal getSizeSDelta() { return sizeSDelta; }
    public void setSizeSDelta(BigDecimal v) { this.sizeSDelta = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSizeMDelta() { return sizeMDelta; }
    public void setSizeMDelta(BigDecimal v) { this.sizeMDelta = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSizeLDelta() { return sizeLDelta; }
    public void setSizeLDelta(BigDecimal v) { this.sizeLDelta = v == null ? BigDecimal.ZERO : v; }

}
