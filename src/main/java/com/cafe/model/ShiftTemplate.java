package com.cafe.model;

import java.time.LocalTime;

/** hr.ShiftTemplate — mẫu ca làm theo chi nhánh (Ca sáng/chiều/tối). */
public class ShiftTemplate {
    private int shiftTemplateId;
    private int branchId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;

    public int getShiftTemplateId() { return shiftTemplateId; }
    public void setShiftTemplateId(int v) { this.shiftTemplateId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { this.endTime = v; }

    /** "07:00–12:00" cho hiển thị. */
    public String getTimeRange() {
        if (startTime == null || endTime == null) return "";
        return startTime + "–" + endTime;
    }
}
