package com.cafe.model;

import com.cafe.common.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** sales.OrderItem — dòng đơn; Status dùng chung cho KDS + tracking khách. */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int productId;
    private int quantity;
    private BigDecimal unitPrice;      // giá tại thời điểm đặt (đã gồm modifier)
    private String note;
    private String status;             // WAITING | MAKING | READY | SERVED | CANCELLED
    private LocalDateTime startedAt;
    private LocalDateTime doneAt;

    // join / hiển thị
    private String productName;
    private String tableNumber;
    private Integer orderBranchId;
    private List<OrderItemModifier> modifiers = new ArrayList<>();
    private int waitedSeconds;
    private Integer makingSeconds;

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int v) { this.orderItemId = v; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int v) { this.orderId = v; }

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime v) { this.startedAt = v; }

    public LocalDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(LocalDateTime v) { this.doneAt = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

    public Integer getOrderBranchId() { return orderBranchId; }
    public void setOrderBranchId(Integer v) { this.orderBranchId = v; }

    public List<OrderItemModifier> getModifiers() { return modifiers; }
    public void setModifiers(List<OrderItemModifier> v) { this.modifiers = v; }

    public int getWaitedSeconds() { return waitedSeconds; }
    public void setWaitedSeconds(int v) { this.waitedSeconds = Math.max(0, v); }

    public Integer getMakingSeconds() { return makingSeconds; }
    public void setMakingSeconds(Integer v) { this.makingSeconds = v == null ? null : Math.max(0, v); }

    public int getWaitedMinutes() { return waitedSeconds / 60; }

    public String getWaitedDisplay() { return formatDuration(waitedSeconds); }

    public Integer getMakingMinutes() {
        return makingSeconds == null ? null : makingSeconds / 60;
    }

    public String getMakingDisplay() {
        return makingSeconds == null ? "" : formatDuration(makingSeconds);
    }

    public String getAgeTier() {
        if (waitedSeconds >= Constants.KDS_CRIT_SECONDS) return "crit";
        if (waitedSeconds >= Constants.KDS_WARN_SECONDS) return "warn";
        return "ok";
    }

    public static String formatDuration(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        int hours = minutes / 60;
        int mins = minutes % 60;
        return hours > 0 ? hours + "h" + mins + "′" : mins + "′";
    }

    public BigDecimal getLineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
