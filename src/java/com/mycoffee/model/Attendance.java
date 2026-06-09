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

    public Attendance() {}
    // Constructor & Getter/Setter
}