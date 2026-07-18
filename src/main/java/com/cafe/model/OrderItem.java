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
    private String status;             // OrderItemStatus
    private LocalDateTime startedAt;
    private LocalDateTime doneAt;
    private LocalDateTime servedAt;
    private LocalDateTime orderCreatedAt;
    private LocalDateTime issueReportedAt;
    private LocalDateTime pickedUpAt;
    private Integer baristaId;
    private Integer preparedBy;
    private Integer issueReportedBy;
    private Integer pickedUpBy;
    private boolean hasIssue;
    private String issueReason;
    private int remakeCount;
    private boolean remakeInventoryReserved;
    private String handoverLocation;

    // join / hiển thị
    private String productName;
    private String tableNumber;
    private String orderType;
    private String categoryName;
    private String baristaName;
    private String preparedByName;
    private String sessionStatus;      // sales.TableSession.Status (OPEN/CLOSED) — nhận biết khách đã thanh toán
    private Integer orderBranchId;
    private List<OrderItemModifier> modifiers = new ArrayList<>();
    private int waitedSeconds;
    private Integer makingSeconds;
    private Integer serveWaitSeconds;   // giây kể từ lúc pha xong (DoneAt) tới hiện tại — SLA màn chờ giao
    private boolean recipeMissing;      // sản phẩm chưa khai báo công thức → backend chặn hoàn thành

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

    public LocalDateTime getServedAt() { return servedAt; }
    public void setServedAt(LocalDateTime v) { this.servedAt = v; }

    public LocalDateTime getOrderCreatedAt() { return orderCreatedAt; }
    public void setOrderCreatedAt(LocalDateTime v) { this.orderCreatedAt = v; }
    public LocalDateTime getIssueReportedAt() { return issueReportedAt; }
    public void setIssueReportedAt(LocalDateTime v) { this.issueReportedAt = v; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime v) { this.pickedUpAt = v; }
    public Integer getBaristaId() { return baristaId; }
    public void setBaristaId(Integer v) { this.baristaId = v; }
    public Integer getPreparedBy() { return preparedBy; }
    public void setPreparedBy(Integer v) { this.preparedBy = v; }
    public Integer getIssueReportedBy() { return issueReportedBy; }
    public void setIssueReportedBy(Integer v) { this.issueReportedBy = v; }
    public Integer getPickedUpBy() { return pickedUpBy; }
    public void setPickedUpBy(Integer v) { this.pickedUpBy = v; }
    public boolean isHasIssue() { return hasIssue; }
    public void setHasIssue(boolean v) { this.hasIssue = v; }
    public String getIssueReason() { return issueReason; }
    public void setIssueReason(String v) { this.issueReason = v; }
    public int getRemakeCount() { return remakeCount; }
    public void setRemakeCount(int v) { this.remakeCount = Math.max(0, v); }
    public boolean isRemakeInventoryReserved() { return remakeInventoryReserved; }
    public void setRemakeInventoryReserved(boolean v) { this.remakeInventoryReserved = v; }
    public String getHandoverLocation() { return handoverLocation; }
    public void setHandoverLocation(String v) { this.handoverLocation = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String v) { this.orderType = v; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String v) { this.categoryName = v; }
    public String getBaristaName() { return baristaName; }
    public void setBaristaName(String v) { this.baristaName = v; }
    public String getPreparedByName() { return preparedByName; }
    public void setPreparedByName(String v) { this.preparedByName = v; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String v) { this.sessionStatus = v; }

    public Integer getOrderBranchId() { return orderBranchId; }
    public void setOrderBranchId(Integer v) { this.orderBranchId = v; }

    public List<OrderItemModifier> getModifiers() { return modifiers; }
    public void setModifiers(List<OrderItemModifier> v) { this.modifiers = v; }

    public int getWaitedSeconds() { return waitedSeconds; }
    public void setWaitedSeconds(int v) { this.waitedSeconds = Math.max(0, v); }

    public Integer getMakingSeconds() { return makingSeconds; }
    public void setMakingSeconds(Integer v) { this.makingSeconds = v == null ? null : Math.max(0, v); }

    public Integer getServeWaitSeconds() { return serveWaitSeconds; }
    public void setServeWaitSeconds(Integer v) { this.serveWaitSeconds = v == null ? null : Math.max(0, v); }

    public boolean isRecipeMissing() { return recipeMissing; }
    public void setRecipeMissing(boolean v) { this.recipeMissing = v; }

    public String getServeWaitDisplay() {
        return serveWaitSeconds == null ? "" : formatMinutesLabel(serveWaitSeconds);
    }

    public int getCupCount() { return quantity; }

    public String getOrderTypeLabel() {
        if ("TAKEAWAY".equals(orderType)) return "Mang đi";
        if ("DELIVERY".equals(orderType)) return "Giao hàng";
        return "Tại bàn";
    }

    public String getStation() {
        String value = ((categoryName == null ? "" : categoryName) + " "
                + (productName == null ? "" : productName)).toLowerCase(java.util.Locale.ROOT);
        if (value.contains("xay") || value.contains("đá xay")) return "BLENDER";
        if (value.contains("trà") || value.contains("tea")) return "TEA";
        return "COFFEE";
    }

    public boolean isOvernight() {
        if (orderCreatedAt == null) return false;
        return orderCreatedAt.toLocalDate().isBefore(java.time.LocalDateTime.now().toLocalDate());
    }

    public String getCreatedDisplay() {
        return orderCreatedAt == null ? "" : orderCreatedAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getStartedDisplay() {
        return startedAt == null ? "" : startedAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getDoneDisplay() {
        return doneAt == null ? "" : doneAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** Nhãn thời gian trên card. Chữ phải là chữ nhân viên nói ra miệng được — không dùng "SLA". */
    public String getSlaLabel() {
        if (isOvernight()) return "Trễ từ hôm qua";
        if (waitedSeconds >= Constants.KDS_CRIT_SECONDS) {
            return "Trễ " + formatMinutesLabel(waitedSeconds - Constants.KDS_SLA_SECONDS);
        }
        if (waitedSeconds >= Constants.KDS_WARN_SECONDS) return "Sắp trễ";
        return "Chờ " + formatMinutesLabel(waitedSeconds);
    }

    public String getSlaTier() {
        if (hasIssue) return "blocked";
        if (isOvernight() || waitedSeconds >= Constants.KDS_SEVERE_SECONDS) return "severe";
        if (waitedSeconds >= Constants.KDS_CRIT_SECONDS) return "crit";
        if (waitedSeconds >= Constants.KDS_WARN_SECONDS) return "warn";
        return "ok";
    }

    public boolean isPriority() { return remakeCount > 0; }

    /** Tier SLA chờ nhân viên nhận món, tính từ DoneAt. */
    public String getServeTier() {
        if (serveWaitSeconds == null) return "ok";
        if (serveWaitSeconds >= Constants.PICKUP_CRIT_SECONDS) return "crit";
        if (serveWaitSeconds >= Constants.PICKUP_WARN_SECONDS) return "warn";
        return "ok";
    }

    public int getWaitedMinutes() { return waitedSeconds / 60; }

    public String getWaitedDisplay() { return formatDuration(waitedSeconds); }

    public Integer getMakingMinutes() {
        return makingSeconds == null ? null : makingSeconds / 60;
    }

    public String getMakingDisplay() {
        return makingSeconds == null ? "" : formatMinutesLabel(makingSeconds);
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

    /**
     * Nhãn thời lượng cho KDS. Dưới 2 tiếng dùng phút (barista nhẩm được ngay);
     * quá đó đổi sang giờ vì "1770 phút" không ai đọc ra là gần 30 tiếng.
     */
    public static String formatMinutesLabel(int seconds) {
        int minutes = Math.max(0, seconds) / 60;
        if (minutes < 120) return minutes + " phút";
        int hours = minutes / 60;
        int mins = minutes % 60;
        return mins == 0 ? hours + " tiếng" : hours + " tiếng " + mins + " phút";
    }

    public BigDecimal getLineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
