package com.cafe.model;

import com.cafe.common.Reason86;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MenuBlockRequest {
    private static final DateTimeFormatter SHORT_DT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private int requestId;
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

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

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

    public Reason86 getReasonEnum() { return reasonEnum; }
    public void setReasonEnum(Reason86 reasonEnum) { this.reasonEnum = reasonEnum; }

    /** Quá hạn dự kiến có lại mà chưa mở bán -> manager cần xử lý gấp. */
    public boolean isOverdue() {
        return closedAt == null && backInEta != null && backInEta.isBefore(LocalDateTime.now());
    }

    public String getReasonLabel() {
        return reasonEnum == null ? "" : reasonEnum.label();
    }

    public String getStatusLabel() {
        if ("PENDING".equals(status)) return "Chờ quản lý duyệt";
        if ("APPROVED".equals(status)) return "Đã duyệt tạm hết";
        if ("REJECTED".equals(status)) return "Đã từ chối";
        if ("RESOLVED".equals(status)) return "Đã mở bán lại";
        return "";
    }

    public String getBackInEtaText() { return format(backInEta); }
    public String getRequestedAtText() { return format(requestedAt); }
    public String getReopenRequestedAtText() { return format(reopenRequestedAt); }
    public String getReviewedAtText() { return format(reviewedAt); }
    public String getClosedAtText() { return format(closedAt); }

    private String format(LocalDateTime dt) {
        return dt == null ? "" : dt.format(SHORT_DT);
    }
}
