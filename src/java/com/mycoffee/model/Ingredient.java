package com.mycoffee.model;

public class Ingredient {
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private String description;

    public Ingredient() {}

    public Ingredient(int ingredientId, String ingredientName, String unit, String description) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.unit = unit;
        this.description = description;
    }

    // Duy tự sinh Getter và Setter (Alt + Insert -> Getter and Setter)
}