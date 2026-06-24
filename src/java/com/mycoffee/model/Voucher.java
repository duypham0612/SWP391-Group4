package com.mycoffee.model;

public class Voucher {
    private int voucherId;
    private String voucherCode;
    private double discountValue;
    private boolean isPercentage;
    private Integer productId;

    // 3 THUỘC TÍNH MỚI THÊM VÀO DB
    private Double maxDiscount; // Dùng Double để hỗ trợ giá trị null
    private int usageLimit;
    private int usedCount;

    public Voucher() {
    }

    public int getVoucherId() { return voucherId; }
    public void setVoucherId(int voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public boolean isIsPercentage() { return isPercentage; }
    public void setIsPercentage(boolean isPercentage) { this.isPercentage = isPercentage; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Double getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Double maxDiscount) { this.maxDiscount = maxDiscount; }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
}