package com.cafe.common;

/** Tên chuẩn của các nhóm lựa chọn POS và ánh xạ tên lưu trữ riêng theo sản phẩm. */
public final class ModifierGroupNames {
    public static final String SIZE = "Size";
    public static final String SUGAR = "Đường";
    public static final String ICE = "Đá";
    private static final String PRODUCT_SIZE_PREFIX = "Size sản phẩm #";

    private ModifierGroupNames() { }

    /** Giá option size thuộc từng sản phẩm nên group vật lý phải có tên unique toàn hệ thống. */
    public static String productSize(int productId) {
        if (productId <= 0) throw new IllegalArgumentException("ProductId phải dương.");
        return PRODUCT_SIZE_PREFIX + productId;
    }

    public static boolean isSize(String name) {
        return SIZE.equals(name) || name != null && name.startsWith(PRODUCT_SIZE_PREFIX);
    }

    public static boolean isStandardChoice(String name) {
        return isSize(name) || SUGAR.equals(name) || ICE.equals(name);
    }

    /** Không để lộ suffix kỹ thuật trên POS, công thức hoặc thông báo validation. */
    public static String display(String storedName) {
        return isSize(storedName) ? SIZE : storedName;
    }
}
