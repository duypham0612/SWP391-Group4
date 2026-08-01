package com.cafe.model;

/**
 * Thông tin cấp ĐƠN gắn lên từng dòng món của hàng chờ quầy pha chế.
 *
 * <p>Cùng một thể hiện được chia sẻ cho mọi dòng thuộc một đơn: hàng chờ vốn là danh sách phẳng
 * theo ly, thiếu lớp này thì barista không biết ly đang cầm là ly cuối của đơn hay còn ly nữa.
 *
 * <p>Số đếm luôn tính trên TOÀN hàng chờ, không phải phần còn sót sau bộ lọc — nếu không, nhãn
 * "còn 1/3" sẽ đổi nghĩa mỗi lần bấm chip lọc.
 */
public class OrderGroupInfo {

    private final int orderId;
    private final String tableNumber;
    private final String pickupCode;
    private final String orderType;
    private int lineCount;          // tổng số dòng món của đơn đang có trên quầy
    private int doneCount;          // số dòng đã pha xong (READY)
    private int waitingCount;       // số dòng còn chờ pha — điều kiện hiện nút "Nhận pha cả đơn"
    private int mineMakingCount;    // số dòng CHÍNH tôi đang pha — điều kiện hiện nút "Xong cả đơn"

    public OrderGroupInfo(int orderId, String tableNumber, String pickupCode, String orderType) {
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.pickupCode = pickupCode;
        this.orderType = orderType;
    }

    /** Cộng dồn một dòng của đơn vào các con số. */
    public void add(String status, boolean mine) {
        lineCount++;
        if ("READY".equals(status)) doneCount++;
        else if ("WAITING".equals(status)) waitingCount++;
        else if ("MAKING".equals(status) && mine) mineMakingCount++;
    }

    public int getOrderId() { return orderId; }
    public String getTableNumber() { return tableNumber; }
    public String getPickupCode() { return pickupCode; }
    public String getOrderType() { return orderType; }
    public int getLineCount() { return lineCount; }
    public int getDoneCount() { return doneCount; }
    public int getWaitingCount() { return waitingCount; }
    public int getMineMakingCount() { return mineMakingCount; }

    /** Số dòng chưa pha xong — chữ hiển thị trên tiêu đề nhóm. */
    public int getPendingCount() { return Math.max(0, lineCount - doneCount); }

    /**
     * Đơn một dòng KHÔNG dựng tiêu đề nhóm: bàn và mã gọi món đã nằm sẵn trên chính dòng đó,
     * thêm tiêu đề chỉ làm danh sách dài gấp đôi ở quán toàn đơn một ly.
     */
    public boolean isGrouped() { return lineCount > 1; }

}
