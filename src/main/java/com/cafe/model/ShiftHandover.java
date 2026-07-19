package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDateTime;

/** hr.ShiftHandover — ghi chú bàn giao ca (Barista, B7). */
public class ShiftHandover {
    private int shiftHandoverId;
    private int branchId;
    private String note;
    private int createdBy;
    private LocalDateTime createdAt;

    // join
    private String createdByName;

    public int getShiftHandoverId() { return shiftHandoverId; }
    public void setShiftHandoverId(int v) { this.shiftHandoverId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String v) { this.createdByName = v; }

    /** Giờ ghi bàn giao theo giờ Việt Nam để JSP hiển thị (createdAt lưu UTC). */
    public String getCreatedDisplay() {
        return BusinessDay.fmtDateTimeVn(createdAt);
    }
}
