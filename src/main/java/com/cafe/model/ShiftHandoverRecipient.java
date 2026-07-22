package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDateTime;

/** Người cần tiếp nhận một bàn giao ca. */
public class ShiftHandoverRecipient {
    private int shiftHandoverRecipientId;
    private int recipientUserId;
    private Integer recipientShiftAssignmentId;
    private String recipientType;
    private LocalDateTime acknowledgedAt;
    private String recipientName;
    private String shiftLabel;

    public int getShiftHandoverRecipientId() { return shiftHandoverRecipientId; }
    public void setShiftHandoverRecipientId(int value) { this.shiftHandoverRecipientId = value; }
    public int getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(int value) { this.recipientUserId = value; }
    public Integer getRecipientShiftAssignmentId() { return recipientShiftAssignmentId; }
    public void setRecipientShiftAssignmentId(Integer value) { this.recipientShiftAssignmentId = value; }
    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String value) { this.recipientType = value; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime value) { this.acknowledgedAt = value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { this.recipientName = value; }
    public String getShiftLabel() { return shiftLabel; }
    public void setShiftLabel(String value) { this.shiftLabel = value; }
    public boolean isAcknowledged() { return acknowledgedAt != null; }
    public String getAcknowledgedDisplay() { return BusinessDay.fmtDateTimeVn(acknowledgedAt); }
}
