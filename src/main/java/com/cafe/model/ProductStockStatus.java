package com.cafe.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

/** Trạng thái bán của một món suy ra từ tồn các nguyên liệu trong công thức. */
public class ProductStockStatus {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String LOW = "LOW";
    public static final String OUT = "OUT";

    private final int productId;
    private String state = AVAILABLE;
    private final Set<String> lowIngredients = new LinkedHashSet<>();
    private final Set<String> outIngredients = new LinkedHashSet<>();

    public ProductStockStatus(int productId) {
        this.productId = productId;
    }

    public void include(String ingredientState, String ingredientName) {
        String name = ingredientName == null || ingredientName.isBlank()
                ? "nguyên liệu chưa xác định" : ingredientName.trim();
        if (OUT.equals(ingredientState)) {
            state = OUT;
            outIngredients.add(name);
        } else if (LOW.equals(ingredientState)) {
            if (!OUT.equals(state)) state = LOW;
            lowIngredients.add(name);
        }
    }

    public int getProductId() { return productId; }
    public String getState() { return state; }
    public boolean isOut() { return OUT.equals(state); }
    public boolean isLow() { return LOW.equals(state); }

    public String getMessage() {
        if (isOut()) return "Hết " + join(outIngredients);
        if (isLow()) return "Sắp hết " + join(lowIngredients);
        return "";
    }

    private static String join(Set<String> names) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String name : names) joiner.add(name);
        return joiner.length() == 0 ? "nguyên liệu" : joiner.toString();
    }
}
