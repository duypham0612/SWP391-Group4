package com.cafe.model;

import com.cafe.common.Reason86;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MenuBlockRequest {
    private int menuBlockRequestId;
    private int branchId;
    private int productId;
    private String reason;
    private String note;
    private LocalDateTime backInEta;
    private int requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime reopenRequestedAt;
    private String status;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime closedAt;

    private String productName;
    private String requesterName;
    private String reviewerName;
    private Reason86 reasonEnum;

    public int getMenuBlockRequestId() { return menuBlockRequestId; }
    public void setMenuBlockRequestId(int menuBlockRequestId) { this.menuBlockRequestId = menuBlockRequestId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getReason() { return reason; }
    public void setReason(String reason) {
        this.reason = reason;
        this.reasonEnum = Reason86.fromCode(reason);
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getBackInEta() { return backInEta; }
    public void setBackInEta(LocalDateTime backInEta) { this.backInEta = backInEta; }

    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getReopenRequestedAt() { return reopenRequestedAt; }
    public void setReopenRequestedAt(LocalDateTime reopenRequestedAt) { this.reopenRequestedAt = reopenRequestedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    /** Quá hạn dự kiến có lại mà chưa mở bán -> manager cần xử lý gấp. */
    public boolean isOverdue() {
        return closedAt == null && backInEta != null
                && backInEta.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

}
