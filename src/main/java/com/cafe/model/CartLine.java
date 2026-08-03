package com.cafe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Một dòng giỏ hàng gửi từ POS hoặc QR, trước khi được ghi thành đơn.
 *
 * <p>Đây là dữ liệu <b>người dùng gửi lên</b>, chưa qua kiểm tra: {@code productId} có thể không tồn
 * tại, {@code quantity} có thể âm, {@code optionIds} có thể không thuộc sản phẩm. Mọi caller phải
 * chạy qua {@code OrderQuantityValidator} (và {@code CashierOrderValidator} nếu là POS) trước khi
 * đặt đơn.
 *
 * <p>Lớp này nằm ở {@code model/} chứ không nằm trong service nào, vì cả ba tầng đều chạm tới nó:
 * {@code web/form} dựng ra, {@code service/cashier} + {@code service/customer} kiểm tra, và
 * {@code OrderPlacementService} tiêu thụ. Trước đây nó là lớp lồng trong facade
 * {@code OrderService} — facade nay đã bị gỡ hẳn ở Đợt 3 — khiến service chuyên trách phải phụ
 * thuộc ngược vào facade cho DTO của chính mình.
 *
 * <p>Vẫn để field public thay vì record: JSP/EL 4.0 chưa đọc được thuộc tính của record, và các
 * caller hiện gán từng field một sau khi khởi tạo.
 */
public class CartLine {
    public int productId;
    public int quantity;
    public String note;
    public List<Integer> optionIds = new ArrayList<>();
}
