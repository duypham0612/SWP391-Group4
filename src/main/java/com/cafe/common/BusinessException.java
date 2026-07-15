package com.cafe.common;

/**
 * Lỗi NGHIỆP VỤ (vi phạm ràng buộc kinh doanh, không phải lỗi hệ thống).
 * Controller nên bắt riêng để hiển thị flash message thân thiện thay vì 500.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
