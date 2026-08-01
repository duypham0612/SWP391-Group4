package com.cafe.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;

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
        String name = ingredientName == null ? "" : ingredientName.trim();
        if (OUT.equals(ingredientState)) {
            state = OUT;
            if (!name.isEmpty()) outIngredients.add(name);
        } else if (LOW.equals(ingredientState)) {
            if (!OUT.equals(state)) state = LOW;
            if (!name.isEmpty()) lowIngredients.add(name);
        }
    }

    public int getProductId() { return productId; }
    public String getState() { return state; }
    public boolean isOut() { return OUT.equals(state); }
    public boolean isLow() { return LOW.equals(state); }
    public Set<String> getLowIngredients() { return Collections.unmodifiableSet(lowIngredients); }
    public Set<String> getOutIngredients() { return Collections.unmodifiableSet(outIngredients); }
}
