package com.cafe.model;

import java.math.BigDecimal;

/** inventory.StockReceiptDetail — dòng phiếu nhập. */
public class StockReceiptDetail {
    private int stockReceiptDetailId;
    private int stockReceiptId;
    private int ingredientId;
    private BigDecimal quantity;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private String unit;            // đơn vị nhập per-line (vd "Túi"); null = dùng đơn vị gốc nguyên liệu

    // join
    private String ingredientName;
    private String ingredientUnit;

    public int getStockReceiptDetailId() { return stockReceiptDetailId; }
    public void setStockReceiptDetailId(int stockReceiptDetailId) { this.stockReceiptDetailId = stockReceiptDetailId; }

    public int getStockReceiptId() { return stockReceiptId; }
    public void setStockReceiptId(int stockReceiptId) { this.stockReceiptId = stockReceiptId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    /** Đơn vị hiển thị: ưu tiên đơn vị nhập per-line, fallback về đơn vị gốc nguyên liệu. */
    public String getDisplayUnit() { return (unit == null || unit.isBlank()) ? ingredientUnit : unit; }

    public BigDecimal getLineCost() {
        return quantity == null || unitCost == null ? BigDecimal.ZERO : quantity.multiply(unitCost);
    }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }
}
