package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** sales.Orders — đơn (COUNTER hoặc QR, cùng một bảng). */
public class Order {
    private int orderId;
    private int branchId;
    private Integer tableSessionId;
    private Integer customerId;
    private String source;             // COUNTER | QR
    private String orderType;          // DINE_IN | TAKEAWAY
    private String status;             // ACTIVE | COMPLETED | CANCELLED
    private Integer createdBy;
    private LocalDateTime createdAt;

    // join / computed
    private String tableNumber;
    private List<OrderItem> items = new ArrayList<>();

    public int getOrderId() { return orderId; }
    public void setOrderId(int v) { this.orderId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public Integer getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Integer v) { this.tableSessionId = v; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer v) { this.customerId = v; }

    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> v) { this.items = v; }

    /** Tổng tiền đơn = Σ(unitPrice × qty). */
    public BigDecimal getTotal() {
        BigDecimal t = BigDecimal.ZERO;
        for (OrderItem it : items) {
            if (!"CANCELLED".equals(it.getStatus()) && it.getUnitPrice() != null) {
                t = t.add(it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }
        return t;
    }
}
