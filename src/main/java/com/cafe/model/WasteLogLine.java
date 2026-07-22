package com.cafe.model;

import java.math.BigDecimal;

/** Input cho một dòng ghi hao hụt nguyên liệu. */
public class WasteLogLine {
    private int ingredientId;
    private BigDecimal quantity;
    private String wasteType;
    private String reason;
    /** Mã nguyên nhân chuẩn của sự cố; không suy diễn từ chuỗi hiển thị ở UI. */
    private String causeCode;

    public WasteLogLine() {}

    public WasteLogLine(int ingredientId, BigDecimal quantity, String wasteType, String reason) {
        this(ingredientId, quantity, wasteType, reason, null);
    }

    public WasteLogLine(int ingredientId, BigDecimal quantity, String wasteType, String reason, String causeCode) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.wasteType = wasteType;
        this.reason = reason;
        this.causeCode = causeCode;
    }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCauseCode() { return causeCode; }
    public void setCauseCode(String causeCode) { this.causeCode = causeCode; }
}
