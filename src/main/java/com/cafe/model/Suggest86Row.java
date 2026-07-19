package com.cafe.model;

/**
 * B3 · Gợi ý 86 (soft) — món CÒN BÁN nhưng có ít nhất một nguyên liệu công thức đã cạn (tồn ≤ 0)
 * tại chi nhánh. Chỉ để gợi ý cho barista cân nhắc báo hết; KHÔNG tự khoá (tránh khoá nhầm hàng
 * loạt khi nguyên liệu dùng chung cạn).
 */
public class Suggest86Row {
    private int productId;
    private String productName;
    private String ingredientName;   // một nguyên liệu đã cạn (đại diện)

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String v) { this.ingredientName = v; }
}
