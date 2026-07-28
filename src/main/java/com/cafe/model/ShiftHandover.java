package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** hr.ShiftHandover — ghi chú bàn giao ca (Barista, B7). */
public class ShiftHandover {
    /**
     * Sau ngưỡng này, nếu chưa người nhận nào xác nhận thì barista đang trực được nhận thay.
     * Mốc tính từ lúc lập bàn giao và dùng chung với điều kiện ghi/đếm trong ShiftHandoverDao.
     */
    public static final int CLAIM_AFTER_HOURS = 4;

    private int shiftHandoverId;
    private int branchId;
    private String note;
    private int createdBy;
    private Integer sourceShiftAssignmentId;
    private String overallStatus;
    private LocalDateTime createdAt;

    // join
    private String createdByName;
    private String sourceShiftLabel;
    private List<ShiftHandoverRecipient> recipients = new ArrayList<>();
    private List<ShiftHandoverTask> tasks = new ArrayList<>();
    private boolean currentUserRecipient;
    private boolean currentUserAcknowledged;
    private boolean currentUserCreator;
    private boolean claimable;

    public int getShiftHandoverId() { return shiftHandoverId; }
    public void setShiftHandoverId(int v) { this.shiftHandoverId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int v) { this.createdBy = v; }

    public Integer getSourceShiftAssignmentId() { return sourceShiftAssignmentId; }
    public void setSourceShiftAssignmentId(Integer v) { this.sourceShiftAssignmentId = v; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String v) { this.overallStatus = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String v) { this.createdByName = v; }
    public String getSourceShiftLabel() { return sourceShiftLabel; }
    public void setSourceShiftLabel(String v) { this.sourceShiftLabel = v; }
    public List<ShiftHandoverRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<ShiftHandoverRecipient> v) { this.recipients = v == null ? new ArrayList<>() : v; }
    public List<ShiftHandoverTask> getTasks() { return tasks; }
    public void setTasks(List<ShiftHandoverTask> v) { this.tasks = v == null ? new ArrayList<>() : v; }
    public boolean isCurrentUserRecipient() { return currentUserRecipient; }
    public void setCurrentUserRecipient(boolean v) { this.currentUserRecipient = v; }
    public boolean isCurrentUserAcknowledged() { return currentUserAcknowledged; }
    public void setCurrentUserAcknowledged(boolean v) { this.currentUserAcknowledged = v; }
    public boolean isCurrentUserCreator() { return currentUserCreator; }
    public void setCurrentUserCreator(boolean v) { this.currentUserCreator = v; }
    /**
     * Bàn giao chưa có người nhận, hoặc đã quá hạn mà chưa ai xác nhận — người đang trực nhận được.
     * Tách khỏi {@link #isCanAcknowledge()} vì xác nhận dành cho người đã được chỉ định, còn nhận
     * thay là tự đứng ra gánh phần việc ca trước đang bị kẹt.
     */
    public boolean isClaimable() { return claimable; }
    public void setClaimable(boolean v) { this.claimable = v; }

    /**
     * Gán các cờ phụ thuộc người đang xem, sau khi người nhận đã được nạp xong.
     * Luật ở đây phải khớp với claimStale/countClaimableInBranch trong ShiftHandoverDao:
     * chỉ WAITING_RECEIPT, chưa ai xác nhận, người xem chưa nằm trong danh sách nhận và không phải
     * người gửi; bàn giao mồ côi nhận ngay, bàn giao đã có người nhận chỉ nhận thay khi quá hạn.
     */
    public void applyViewer(int currentUserId) {
        this.currentUserCreator = createdBy == currentUserId;
        boolean noneAcknowledged = getAcknowledgedCount() == 0;
        boolean stale = createdAt != null
                && createdAt.isBefore(LocalDateTime.now(ZoneOffset.UTC).minusHours(CLAIM_AFTER_HOURS));
        this.claimable = "WAITING_RECEIPT".equals(overallStatus)
                && createdBy != currentUserId
                && !currentUserRecipient
                && noneAcknowledged
                && (recipients.isEmpty() || stale);
    }
    public boolean isCanAcknowledge() { return currentUserRecipient && !currentUserAcknowledged; }
    public boolean isCanUpdateTasks() { return currentUserRecipient && currentUserAcknowledged; }
    public boolean isLegacy() { return "LEGACY".equals(overallStatus); }

    // Tiến độ tính từ danh sách đã nạp sẵn — không thêm truy vấn nào cho màn danh sách.
    public int getTaskCount() { return tasks.size(); }
    public int getDoneTaskCount() { int done = 0; for (ShiftHandoverTask t : tasks) if ("DONE".equals(t.getStatus())) done++; return done; }
    public int getOpenTaskCount() { return getTaskCount() - getDoneTaskCount(); }
    public int getProgressPercent() { return tasks.isEmpty() ? 0 : getDoneTaskCount() * 100 / tasks.size(); }
    public int getRecipientCount() { return recipients.size(); }
    public int getAcknowledgedCount() { int ack = 0; for (ShiftHandoverRecipient r : recipients) if (r.isAcknowledged()) ack++; return ack; }
    /** Còn người nhận chưa bấm xác nhận — dùng để tô cảnh báo cho cả người gửi lẫn người nhận. */
    public boolean isAwaitingReceipt() { return !recipients.isEmpty() && getAcknowledgedCount() < recipients.size(); }
    public String getOverallStatusLabel() {
        if ("WAITING_RECEIPT".equals(overallStatus)) return "Chờ ca nhận";
        if ("IN_PROGRESS".equals(overallStatus)) return "Đang xử lý";
        if ("COMPLETED".equals(overallStatus)) return "Hoàn tất";
        return "Lịch sử cũ";
    }
    public String getOverallStatusBadge() {
        if ("WAITING_RECEIPT".equals(overallStatus)) return "badge-waiting";
        if ("IN_PROGRESS".equals(overallStatus)) return "badge-making";
        if ("COMPLETED".equals(overallStatus)) return "badge-ready";
        return "badge-cancelled";
    }

    /** Giờ ghi bàn giao theo giờ Việt Nam để JSP hiển thị (createdAt lưu UTC). */
    public String getCreatedDisplay() {
        return BusinessDay.fmtDateTimeVn(createdAt);
    }

    /**
     * Khoảng cách tới hiện tại ("2 giờ trước") — ca nhận cần biết bàn giao còn nóng hay đã tồn lâu,
     * mốc giờ tuyệt đối một mình không nói lên điều đó khi lướt danh sách.
     */
    public String getAgeDisplay() {
        if (createdAt == null) return "";
        long minutes = Duration.between(createdAt, LocalDateTime.now(ZoneOffset.UTC)).toMinutes();
        if (minutes < 1) return "vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (minutes < 60 * 24) return minutes / 60 + " giờ trước";
        return minutes / (60 * 24) + " ngày trước";
    }
}
