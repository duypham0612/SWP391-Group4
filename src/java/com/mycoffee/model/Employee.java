package com.mycoffee.model;

public class Employee {
    private int id;
    private String fullName;
    private String email;
    private String roleName;
    private String status;
    private String shiftInfo;

    public Employee() {
    }

    public Employee(int id, String fullName, String email, String roleName, String status, String shiftInfo) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roleName = roleName;
        this.status = status;
        this.shiftInfo = shiftInfo;
    }

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShiftInfo() { return shiftInfo; }
    public void setShiftInfo(String shiftInfo) { this.shiftInfo = shiftInfo; }
}