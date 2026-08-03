package com.cafe.model;

import java.math.BigDecimal;

/** inventory.StockReceiptLine — dòng phiếu nhập. */
public class StockReceiptDetail {
    private int stockReceiptLineId;
    private String receiptBatchId;
    private int ingredientId;
    /** Lựa chọn tạm từ form: 0=đơn vị gốc, 1=đơn vị mua; không lưu DB. */
    private int unitChoice;
    private BigDecimal enteredQuantity;
    private BigDecimal baseQuantity;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private String unitNameAtEntry;
    private BigDecimal factorToBaseAtEntry;

    // join
    private String ingredientName;
    private String ingredientUnit;

    public int getStockReceiptLineId() { return stockReceiptLineId; }
    public void setStockReceiptLineId(int stockReceiptLineId) { this.stockReceiptLineId = stockReceiptLineId; }

    public String getReceiptBatchId() { return receiptBatchId; }
    public void setReceiptBatchId(String receiptBatchId) { this.receiptBatchId = receiptBatchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getEnteredQuantity() { return enteredQuantity; }
    public void setEnteredQuantity(BigDecimal value) { enteredQuantity = value; }
    public BigDecimal getBaseQuantity() { return baseQuantity; }
    public void setBaseQuantity(BigDecimal value) { baseQuantity = value; }
    public int getUnitChoice() { return unitChoice; }
    public void setUnitChoice(int value) { unitChoice = value; }
    public BigDecimal getFactorToBaseAtEntry() { return factorToBaseAtEntry; }
    public void setFactorToBaseAtEntry(BigDecimal value) { factorToBaseAtEntry = value; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getUnitNameAtEntry() { return unitNameAtEntry; }
    public void setUnitNameAtEntry(String value) { unitNameAtEntry = value; }

    public BigDecimal getLineCost() {
        return enteredQuantity == null || unitCost == null ? BigDecimal.ZERO : enteredQuantity.multiply(unitCost);
    }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }
}
