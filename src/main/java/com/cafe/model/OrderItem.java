package com.cafe.model;

import com.cafe.common.BusinessDay;
import com.cafe.common.Constants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** sales.OrderItem — dòng đơn; Status dùng chung cho KDS + tracking khách. */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int branchId;
    private int productId;
    private int quantity;
    private BigDecimal unitPrice;      // giá sản phẩm tại thời điểm đặt; modifier snapshot ở OrderItemModifier
    private Integer billId;
    private BigDecimal billedAmount;   // snapshot chốt bill, tuyệt đối không tính lại khi đọc bill cũ
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

    // join / hiển thị
    private String productNameAtOrder;
    private String tableNumber;
    private String pickupCode;         // mã gọi món của đơn (join hiển thị)
    private String orderType;
    private String categoryName;
    private String baristaName;
    private String preparedByName;
    private String tableStatus;        // sales.DiningTable.Status (EMPTY/OCCUPIED)
    private Integer orderBranchId;
    private List<OrderItemModifier> modifiers = new ArrayList<>();
    private int waitedSeconds;
    private Integer makingSeconds;
    private Integer serveWaitSeconds;   // giây kể từ lúc pha xong (DoneAt) tới hiện tại — SLA màn chờ giao
    private boolean recipeMissing;      // sản phẩm chưa khai báo công thức → backend chặn hoàn thành
    private int prepSeconds = Constants.KDS_SLA_SECONDS;  // thời gian pha chuẩn của món (catalog.Product); mặc định 12' giữ hành vi cũ
    private int seqNo;                  // số thứ tự pha ở chế độ cao điểm (0 = không hiển thị)
    private int orderLineNo;            // dòng thứ mấy trong đơn (1-based) — nhãn "món 2/3"
    private OrderGroupInfo groupInfo;   // thông tin cấp đơn, dùng chung giữa các dòng cùng đơn
    private boolean groupStart;         // dòng mở đầu một khối trên danh sách ĐANG render
    private boolean groupMember;        // dòng nằm trong một khối ĐANG render (để vẽ vạch nối)
    private boolean ownerOffDuty;       // món đang pha mà chủ món đã rời ca → cho phép thu hồi

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int v) { this.orderItemId = v; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int v) { this.orderId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getProductId() { return productId; }
    public void setProductId(int v) { this.productId = v; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }

    public Integer getBillId() { return billId; }
    public void setBillId(Integer v) { this.billId = v; }

    public BigDecimal getBilledAmount() { return billedAmount; }
    public void setBilledAmount(BigDecimal v) { this.billedAmount = v; }

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

    public String getProductName() { return productNameAtOrder; }
    public void setProductName(String v) { this.productNameAtOrder = v; }
    public String getProductNameAtOrder() { return productNameAtOrder; }
    public void setProductNameAtOrder(String v) { this.productNameAtOrder = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

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

    public String getTableStatus() { return tableStatus; }
    public void setTableStatus(String v) { this.tableStatus = v; }

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

    public int getPrepSeconds() { return prepSeconds; }
    public void setPrepSeconds(int v) { this.prepSeconds = v; }

    public int getSeqNo() { return seqNo; }
    public void setSeqNo(int v) { this.seqNo = v; }

    public int getOrderLineNo() { return orderLineNo; }
    public void setOrderLineNo(int v) { this.orderLineNo = v; }

    public OrderGroupInfo getGroupInfo() { return groupInfo; }
    public void setGroupInfo(OrderGroupInfo v) { this.groupInfo = v; }

    public boolean isGroupStart() { return groupStart; }
    public void setGroupStart(boolean v) { this.groupStart = v; }

    public boolean isGroupMember() { return groupMember; }
    public void setGroupMember(boolean v) { this.groupMember = v; }

    public boolean isOwnerOffDuty() { return ownerOffDuty; }
    public void setOwnerOffDuty(boolean v) { this.ownerOffDuty = v; }

    /** Dòng này có thuộc một đơn nhiều món không — quyết định hiện nhãn "món 2/3". */
    public boolean isGrouped() { return groupInfo != null && groupInfo.isGrouped(); }

    public int getCupCount() { return quantity; }

    public String getStation() {
        String value = ((categoryName == null ? "" : categoryName) + " "
                + (productNameAtOrder == null ? "" : productNameAtOrder)).toLowerCase(java.util.Locale.ROOT);
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

    /**
     * Món pha xong đã nằm chờ quá lâu → cảnh báo chất lượng (đồ nguội, đá tan).
     * Tách khỏi bậc SLA hàng chờ vì đây là vấn đề của khâu bàn giao, không phải khâu pha.
     */
    public boolean isStaleReady() {
        return serveWaitSeconds != null && serveWaitSeconds >= Constants.PICKUP_CRIT_SECONDS;
    }

    public boolean isPriority() { return remakeCount > 0; }

    public int getWaitedMinutes() { return waitedSeconds / 60; }

    public Integer getMakingMinutes() {
        return makingSeconds == null ? null : makingSeconds / 60;
    }

    public BigDecimal getLineTotal() {
        if (billedAmount != null) return billedAmount;
        BigDecimal total = unitPrice == null
                ? BigDecimal.ZERO
                : unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (modifiers != null) {
            for (OrderItemModifier modifier : modifiers) {
                if (modifier != null && modifier.getPriceDelta() != null) {
                    total = total.add(modifier.getPriceDelta());
                }
            }
        }
        return total;
    }
}
