package com.cafe.common;

/**
 * Lỗi nghiệp vụ có cấu trúc khi món không còn nhận đặt.
 * Controller dùng lỗi này để trả HTTP 409 thay vì gom chung vào lỗi dữ liệu 400.
 */
public class ItemUnavailableException extends IllegalArgumentException {
    private final int productId;
    private final String productName;
    private final String state;
    private final String reason;

    public ItemUnavailableException(int productId, String productName, String state, String reason) {
        super(reason);
        this.productId = productId;
        this.productName = productName == null ? "#" + productId : productName;
        this.state = state == null ? "OUT" : state;
        this.reason = reason == null ? "Món hiện không thể đặt." : reason;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getState() { return state; }
    public String getReason() { return reason; }
}
