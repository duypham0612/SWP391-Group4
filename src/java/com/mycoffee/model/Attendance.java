package com.mycoffee.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Attendance {

    private int attendanceId;
    private int employeeId;
    private int shiftId;
    private Date date;
    private Timestamp checkInTime;
    private Timestamp checkOutTime;
    private String status;

    // Thuộc tính bổ sung để hiển thị ra màn hình quản lý
    private String employeeName;
    private String shiftName;

    public Attendance() {
    }

    public Attendance(int attendanceId, int employeeId, int shiftId, Date date, Timestamp checkInTime, Timestamp checkOutTime, String status, String employeeName, String shiftName) {
        this.attendanceId = attendanceId;
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.date = date;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.employeeName = employeeName;
        this.shiftName = shiftName;
    }

    public int getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(int attendanceId) {
        this.attendanceId = attendanceId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getShiftId() {
        return shiftId;
    }

    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Timestamp getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Timestamp checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Timestamp getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Timestamp checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

}
