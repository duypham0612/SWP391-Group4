package com.mycoffee.model;

import java.sql.Timestamp;

public class Inventory {

    private int branchId;
    private int ingredientId;
    private double quantity;
    private double minRequired;
    private Timestamp lastUpdated;

    // Thuộc tính bổ sung (Không có trong bảng nhưng dùng để hiển thị tên nguyên liệu/đơn vị lên web cho Manager nhìn)
    private String ingredientName;
    private String unit;

    public Inventory() {
    }

    public Inventory(int branchId, int ingredientId, double quantity, double minRequired, Timestamp lastUpdated, String ingredientName, String unit) {
        this.branchId = branchId;
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.minRequired = minRequired;
        this.lastUpdated = lastUpdated;
        this.ingredientName = ingredientName;
        this.unit = unit;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getMinRequired() {
        return minRequired;
    }

    public void setMinRequired(double minRequired) {
        this.minRequired = minRequired;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
