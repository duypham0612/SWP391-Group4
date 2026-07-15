package com.cafe.model;

/** sales.DiningTable (+ phiên đang mở để vẽ sơ đồ bàn). */
public class DiningTable {
    private int diningTableId;
    private int branchId;
    private String tableNumber;
    private String qrCode;
    private String status;             // EMPTY | OCCUPIED | CLEANING

    // join: phiên đang OPEN (nếu có)
    private Integer activeSessionId;
    private Integer activeItemCount;

    public int getDiningTableId() { return diningTableId; }
    public void setDiningTableId(int v) { this.diningTableId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String v) { this.qrCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Integer getActiveSessionId() { return activeSessionId; }
    public void setActiveSessionId(Integer v) { this.activeSessionId = v; }

    public Integer getActiveItemCount() { return activeItemCount; }
    public void setActiveItemCount(Integer v) { this.activeItemCount = v; }

    public boolean isOccupied() { return activeSessionId != null; }
}
