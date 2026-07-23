package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** hr.ShiftHandover — ghi chú bàn giao ca (Barista, B7). */
public class ShiftHandover {
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
    public boolean isCanAcknowledge() { return currentUserRecipient && !currentUserAcknowledged; }
    public boolean isCanUpdateTasks() { return currentUserRecipient && currentUserAcknowledged; }
    public boolean isLegacy() { return "LEGACY".equals(overallStatus); }
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
}
