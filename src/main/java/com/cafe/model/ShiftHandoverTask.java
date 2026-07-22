package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDateTime;

/** Một đầu việc còn lại của bàn giao ca. */
public class ShiftHandoverTask {
    private int shiftHandoverTaskId;
    private String content;
    private String status;
    private Integer updatedBy;
    private LocalDateTime updatedAt;
    private String updatedByName;

    public int getShiftHandoverTaskId() { return shiftHandoverTaskId; }
    public void setShiftHandoverTaskId(int value) { this.shiftHandoverTaskId = value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer value) { this.updatedBy = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String value) { this.updatedByName = value; }
    public String getUpdatedDisplay() { return BusinessDay.fmtDateTimeVn(updatedAt); }
    public String getStatusLabel() {
        if ("IN_PROGRESS".equals(status)) return "Đang xử lý";
        if ("DONE".equals(status)) return "Đã xử lý";
        return "Mới";
    }
    public String getStatusBadge() {
        if ("IN_PROGRESS".equals(status)) return "badge-making";
        if ("DONE".equals(status)) return "badge-ready";
        return "badge-waiting";
    }
}
