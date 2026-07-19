package com.cafe.model;

import java.io.Serializable;

/**
 * iam.[User] + thông tin join (roleCode, roleName, branchName) phục vụ hiển thị/RBAC.
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
    private int roleId;
    private Integer branchId;      // NULL với Admin (toàn chuỗi)
    private String status;

    // join
    private String roleCode;
    private String roleName;
    private String branchName;

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

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
}
