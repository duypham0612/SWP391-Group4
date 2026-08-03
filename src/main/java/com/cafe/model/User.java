package com.cafe.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * iam.UserAccount + thông tin join (roleCode, roleName, branchName) phục vụ hiển thị/RBAC.
 * Lưu vào session sau khi đăng nhập (đã xoá passwordHash).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String passwordHash;   // chỉ dùng lúc xác thực, không giữ trong session
    private String fullName;
    private String email;
    private String phone;
    private Integer branchId;      // NULL với Admin (toàn chuỗi)
    private BigDecimal hourlyRate;
    private String status;

    // join
    private String roleCode;
    private String roleName;
    private String branchName;
    private Boolean branchActive;
    /** Chi nhánh của nhân sự hiện đã có người quản lý phụ trách hay chưa. */
    private Boolean branchHasManager;
    /** Chính nhân sự này có phải là người quản lý đang được gán cho chi nhánh hay không. */
    private Boolean assignedBranchManager;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public Boolean getBranchActive() { return branchActive; }
    public void setBranchActive(Boolean branchActive) { this.branchActive = branchActive; }

    public Boolean getBranchHasManager() { return branchHasManager; }
    public void setBranchHasManager(Boolean branchHasManager) { this.branchHasManager = branchHasManager; }

    public Boolean getAssignedBranchManager() { return assignedBranchManager; }
    public void setAssignedBranchManager(Boolean assignedBranchManager) {
        this.assignedBranchManager = assignedBranchManager;
    }
}
