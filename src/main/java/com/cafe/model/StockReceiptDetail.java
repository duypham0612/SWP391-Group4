package com.cafe.model;

import java.math.BigDecimal;

/** inventory.StockReceiptDetail — dòng phiếu nhập. */
public class StockReceiptDetail {
    private int stockReceiptDetailId;
    private int stockReceiptId;
    private int ingredientId;
    private int ingredientUnitConversionId;
    private BigDecimal enteredQuantity;
    private BigDecimal baseQuantity;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private String unitNameAtEntry;
    private BigDecimal factorToBaseAtEntry;

    // join
    private String ingredientName;
    private String ingredientUnit;

    public int getStockReceiptDetailId() { return stockReceiptDetailId; }
    public void setStockReceiptDetailId(int stockReceiptDetailId) { this.stockReceiptDetailId = stockReceiptDetailId; }

    public int getStockReceiptId() { return stockReceiptId; }
    public void setStockReceiptId(int stockReceiptId) { this.stockReceiptId = stockReceiptId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getEnteredQuantity() { return enteredQuantity; }
    public void setEnteredQuantity(BigDecimal value) { enteredQuantity = value; }
    public BigDecimal getBaseQuantity() { return baseQuantity; }
    public void setBaseQuantity(BigDecimal value) { baseQuantity = value; }
    public int getIngredientUnitConversionId() { return ingredientUnitConversionId; }
    public void setIngredientUnitConversionId(int value) { ingredientUnitConversionId = value; }
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
