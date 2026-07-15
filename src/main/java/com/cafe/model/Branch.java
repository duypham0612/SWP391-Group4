package com.cafe.model;

import java.time.LocalTime;

/** org.Branch */
public class Branch {
    private int branchId;
    private String code;
    private String name;
    private String address;
    private String phone;
    private boolean active = true;
    private LocalTime openTime;        // A3.F2 — giờ mở cửa (NULL = chưa đặt)
    private LocalTime closeTime;       // A3.F2 — giờ đóng cửa
    private Integer managerUserId;     // A2.F6/A3.F5 — quản lý phụ trách (NULL = chưa gán)
    private String managerName;        // join hiển thị

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }

    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }

    public Integer getManagerUserId() { return managerUserId; }
    public void setManagerUserId(Integer managerUserId) { this.managerUserId = managerUserId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    /** Giờ hoạt động gọn cho JSP: "07:00–22:00" hoặc rỗng nếu chưa đặt. */
    public String getHoursText() {
        if (openTime == null || closeTime == null) return "";
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return openTime.format(f) + "–" + closeTime.format(f);
    }
}
