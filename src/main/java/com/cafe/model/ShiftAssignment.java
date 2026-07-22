package com.cafe.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** hr.ShiftAssignment — phân công 1 nhân viên vào 1 ca trong 1 ngày (+ join để hiển thị). */
public class ShiftAssignment {
    private int shiftAssignmentId;
    private int shiftTemplateId;
    private int userId;
    private LocalDate workDate;

    // join hiển thị
    private String templateName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String userName;
    private String roleCode;

    public int getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(int v) { this.shiftAssignmentId = v; }

    public int getShiftTemplateId() { return shiftTemplateId; }
    public void setShiftTemplateId(int v) { this.shiftTemplateId = v; }

    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String v) { this.templateName = v; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { this.endTime = v; }

    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String v) { this.roleCode = v; }
}
