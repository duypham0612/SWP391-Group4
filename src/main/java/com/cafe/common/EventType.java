package com.cafe.common;

/** Loại domain event ghi vào ops.OutboxEvent (đặc tả mục 2 + 3). */
public enum EventType {
    ORDER_CREATED("order.created"),
    ORDER_STATUS_CHANGED("order.status_changed"),
    PAYMENT_COMPLETED("payment.completed"),
    INVENTORY_DEDUCTED("inventory.deducted"),
    STOCK_LOW("stock.low"),
    STOCK_OVERSOLD("stock.oversold"),   // tồn âm: bán/dùng quá tồn lý thuyết — cần đối soát
    MENU_86_CHANGED("menu.86_changed"), // Barista bật/tắt hết món (khoá/mở khỏi POS + QR) — audit ai/khi nào
    ITEM_READY("item.ready"),
    ITEM_ISSUE_REPORTED("item.issue_reported"),
    ITEM_REMAKE_REQUESTED("item.remake_requested"),
    ITEM_PICKED_UP("item.picked_up"),
    SERVICE_CALL("service.call"),
    BILL_REQUESTED("bill.requested"),
    BILL_VOIDED("bill.voided"),
    BILL_REFUNDED("bill.refunded");

    private final String wire;

    EventType(String wire) { this.wire = wire; }

    /** Chuỗi lưu vào cột EventType (vd "order.created"). */
    public String wire() { return wire; }
}
