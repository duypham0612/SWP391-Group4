package com.mycoffee.model;

public class Employee {
    private int id;
    private String username;   // THÊM: Đồng bộ với ${employee.username}
    private String password;   // THÊM: Cần cho chức năng thêm mới/đăng nhập
    private String fullName;
    private String email;
    private String phone;      // THÊM: Đồng bộ với ${employee.phone}
    private int roleId;        // THÊM: Đồng bộ với ${employee.roleId}
    private String roleName;
    private String status;
    private String shiftInfo;
    private int shiftId; 

    // 1. Constructor mặc định (bắt buộc cho JavaBeans)
    public Employee() {
    }

    // 2. Constructor đầy đủ tham số
    public Employee(int id, String username, String password, String fullName, String email, String phone, int roleId, String roleName, String status, String shiftInfo, int shiftId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.roleId = roleId;
        this.roleName = roleName;
        this.status = status;
        this.shiftInfo = shiftInfo;
        this.shiftId = shiftId;
    }

    // --- GETTERS VÀ SETTERS ---
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // Thêm Getter/Setter cho username để hết lỗi PropertyNotFoundException
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Thêm Getter/Setter cho phone
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // Thêm Getter/Setter cho roleId để logic so sánh <option selected> chạy đúng
    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShiftInfo() { return shiftInfo; }
    public void setShiftInfo(String shiftInfo) { this.shiftInfo = shiftInfo; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }
}
