package com.mycoffee.model;

public class Role {
    private int roleId;
    private String roleName;
    private String description;

    // 1. Constructor không tham số (Mặc định bắt buộc phải có)
    public Role() {
    }

    // 2. Constructor đầy đủ tham số
    public Role(int roleId, String roleName, String description) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
    }

    // 3. Toàn bộ các hàm Getter và Setter để Controller và DAO gọi tới
    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // 4. Hàm toString hỗ trợ việc in test kiểm tra dữ liệu nếu cần
    @Override
    public String toString() {
        return "Role{" + "roleId=" + roleId + ", roleName=" + roleName + ", description=" + description + '}';
    }
}