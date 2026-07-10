package com.cafe.model;

import java.math.BigDecimal;

/** Input cho một dòng ghi hao hụt nguyên liệu. */
public class WasteLogLine {
    private int ingredientId;
    private BigDecimal quantity;
    private String wasteType;
    private String reason;

    public WasteLogLine() {}

    public WasteLogLine(int ingredientId, BigDecimal quantity, String wasteType, String reason) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.wasteType = wasteType;
        this.reason = reason;
    }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
