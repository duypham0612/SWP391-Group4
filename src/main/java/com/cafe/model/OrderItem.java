package com.cafe.model;

import com.cafe.common.BusinessDay;
import com.cafe.common.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** sales.OrderItem — dòng đơn; Status dùng chung cho KDS + tracking khách. */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int productId;
    private int quantity;
    private BigDecimal unitPrice;      // giá tại thời điểm đặt
    private String size = "M";
    private String iceLevel = "Bình thường";
    private String sugarLevel = "100%";
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
    private Integer tableSessionId;    // sales.Orders.TableSessionId — khóa gom nhóm dine-in
    private String pickupCode;         // mã gọi món của đơn (join hiển thị)
    private String orderType;
    private String categoryName;
    private String baristaName;
    private String preparedByName;
    private String sessionStatus;      // sales.TableSession.Status (OPEN/CLOSED) — nhận biết khách đã thanh toán
    private Integer orderBranchId;
    private int waitedSeconds;
    private Integer makingSeconds;
    private Integer serveWaitSeconds;   // giây kể từ lúc pha xong (DoneAt) tới hiện tại — SLA màn chờ giao
    private boolean recipeMissing;      // sản phẩm chưa khai báo công thức → backend chặn hoàn thành
    private int prepSeconds = Constants.KDS_SLA_SECONDS;  // thời gian pha chuẩn của món (catalog.Product); mặc định 12' giữ hành vi cũ
    private int seqNo;                  // số thứ tự pha ở chế độ cao điểm (0 = không hiển thị)

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

    public String getSize() { return size; }
    public void setSize(String v) { this.size = v == null || v.isBlank() ? "M" : v; }

    public String getIceLevel() { return iceLevel; }
    public void setIceLevel(String v) { this.iceLevel = v == null || v.isBlank() ? "Bình thường" : v; }

    public String getSugarLevel() { return sugarLevel; }
    public void setSugarLevel(String v) { this.sugarLevel = v == null || v.isBlank() ? "100%" : v; }

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

    public Integer getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Integer v) { this.tableSessionId = v; }

    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String v) { this.pickupCode = v; }

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

    public int getWaitedSeconds() { return waitedSeconds; }
    public void setWaitedSeconds(int v) { this.waitedSeconds = Math.max(0, v); }

    public Integer getMakingSeconds() { return makingSeconds; }
    public void setMakingSeconds(Integer v) { this.makingSeconds = v == null ? null : Math.max(0, v); }

    public Integer getServeWaitSeconds() { return serveWaitSeconds; }
    public void setServeWaitSeconds(Integer v) { this.serveWaitSeconds = v == null ? null : Math.max(0, v); }

    public boolean isRecipeMissing() { return recipeMissing; }
    public void setRecipeMissing(boolean v) { this.recipeMissing = v; }

    public int getPrepSeconds() { return prepSeconds; }
    public void setPrepSeconds(int v) { this.prepSeconds = v; }

    public int getSeqNo() { return seqNo; }
    public void setSeqNo(int v) { this.seqNo = v; }

    /** Mốc pha chuẩn dùng để tính trễ: theo món; số bất thường (chưa nạp/0) thì lùi về mặc định 12'. */
    private int effectivePrepSeconds() {
        return prepSeconds >= 60 ? prepSeconds : Constants.KDS_SLA_SECONDS;
    }

    public String getServeWaitDisplay() {
        return serveWaitSeconds == null ? "" : formatMinutesLabel(serveWaitSeconds);
    }

    public int getCupCount() { return quantity; }

    public String getOrderTypeLabel() {
        if ("TAKEAWAY".equals(orderType)) return "Mang đi";
        if ("DELIVERY".equals(orderType)) return "Giao hàng";
        return "Tại bàn";
    }

    /** Khóa gom KDS: dine-in gom theo lượt bàn; các loại đơn khác gom theo đơn. */
    public String getBrewGroupKey() {
        return "DINE_IN".equals(orderType) && tableSessionId != null
                ? "T" + tableSessionId : "O" + orderId;
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
        // orderCreatedAt lưu theo UTC — phải quy về giờ VN mới so ngày cho đúng,
        // không so thẳng với đồng hồ máy chủ (dễ lệch 1 ngày quanh nửa đêm).
        java.time.LocalDate createdVn = orderCreatedAt.atZone(java.time.ZoneOffset.UTC)
                .withZoneSameInstant(BusinessDay.VN_ZONE).toLocalDate();
        return createdVn.isBefore(java.time.LocalDate.now(BusinessDay.VN_ZONE));
    }

    public String getCreatedDisplay() {
        return BusinessDay.fmtTimeVn(orderCreatedAt);
    }

    public String getStartedDisplay() {
        return BusinessDay.fmtTimeVn(startedAt);
    }

    public String getDoneDisplay() {
        return BusinessDay.fmtTimeVn(doneAt);
    }

    /**
     * Nhãn thời gian CHÍNH trên card — chỉ một con số để liếc 1 giây là quyết được:
     * còn bao lâu, hay đã quá bao lâu. Phần "đã chờ X/12 phút" nằm ở dòng phụ
     * ({@link #getWaitProgressLabel()}) vì biết đã-chờ và định-mức thì còn-lại tự suy ra;
     * ba con số cùng cỡ sẽ cạnh tranh với tên món, đúng lỗi ưu tiên ngược cần tránh.
     */
    public String getSlaLabel() {
        if (isOvernight()) return "Trễ từ hôm qua";
        int remaining = effectivePrepSeconds() - waitedSeconds;
        // Phút làm tròn xuống, nên cả phút đầu tiên hai bên vạch đều ra "0 phút" —
        // "Trễ 0 phút" thì vô nghĩa, nói thẳng là vừa chạm hạn.
        if (remaining <= 0 && remaining > -60) return "Vừa quá hạn";
        if (remaining <= 0) return "Trễ " + formatMinutesLabel(-remaining);
        if (remaining < 60) return "Sắp hết giờ";
        return "Còn " + formatMinutesLabel(remaining);
    }

    /** Dòng phụ: đã chờ bao lâu so với mốc pha chuẩn CỦA MÓN. Gọn dạng "5/12 phút". */
    public String getWaitProgressLabel() {
        return "Đã chờ " + (Math.max(0, waitedSeconds) / 60)
                + "/" + (effectivePrepSeconds() / 60) + " phút";
    }

    /**
     * Món pha xong đã nằm chờ quá lâu → cảnh báo chất lượng (đồ nguội, đá tan).
     * Tách khỏi bậc SLA hàng chờ vì đây là vấn đề của khâu bàn giao, không phải khâu pha.
     */
    public boolean isStaleReady() {
        return serveWaitSeconds != null && serveWaitSeconds >= Constants.PICKUP_CRIT_SECONDS;
    }

    /**
     * Chỉ ba bậc: bình thường / sắp trễ / đã trễ. Bỏ bậc "severe" cũ vì đơn qua đêm nay nằm
     * ở khu "Đơn treo" riêng — giữ nó lại thì mọi card đều đỏ chỉ vì dữ liệu cũ, và cảnh báo
     * lúc nào cũng bật thì không còn là cảnh báo. Đỏ dành riêng cho quá giờ thật hoặc sự cố thật.
     */
    public String getSlaTier() {
        if (hasIssue) return "blocked";
        int prep = effectivePrepSeconds();
        if (waitedSeconds >= prep) return "late";           // quá mốc pha chuẩn của chính món này
        if (waitedSeconds >= prep * 2 / 3) return "warn";   // sắp tới hạn (2/3 chặng)
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
