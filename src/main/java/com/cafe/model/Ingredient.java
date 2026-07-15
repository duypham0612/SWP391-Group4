package com.cafe.model;

/** catalog.Ingredient — RAW (mua về) hoặc PREPPED (pha sẵn tại quán). */
public class Ingredient {
    private int ingredientId;
    private String name;
    private String unit;            // g, ml, cái, kg, L...
    private String ingredientType;  // RAW | PREPPED
    private boolean active = true;

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
