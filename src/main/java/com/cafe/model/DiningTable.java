package com.cafe.model;

/** sales.DiningTable (+ phiên đang mở để vẽ sơ đồ bàn). */
public class DiningTable {
    private int diningTableId;
    private int branchId;
    private String tableNumber;
    private String qrCode;
    private String status;             // EMPTY | OCCUPIED | CLEANING
    private int capacity;
    private boolean visible;
    private Integer mergedIntoTableId;

    // join: phiên đang OPEN (nếu có)
    private Integer activeSessionId;
    private Integer activeItemCount;
    private String mergedIntoTableNumber;
    private int effectiveCapacity;

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

    public int getCapacity() { return capacity; }
    public void setCapacity(int v) { this.capacity = v; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public Integer getMergedIntoTableId() { return mergedIntoTableId; }
    public void setMergedIntoTableId(Integer v) { this.mergedIntoTableId = v; }

    public Integer getActiveSessionId() { return activeSessionId; }
    public void setActiveSessionId(Integer v) { this.activeSessionId = v; }

    public Integer getActiveItemCount() { return activeItemCount; }
    public void setActiveItemCount(Integer v) { this.activeItemCount = v; }

    public String getMergedIntoTableNumber() { return mergedIntoTableNumber; }
    public void setMergedIntoTableNumber(String v) { this.mergedIntoTableNumber = v; }

    public int getEffectiveCapacity() { return effectiveCapacity > 0 ? effectiveCapacity : capacity; }
    public void setEffectiveCapacity(int v) { this.effectiveCapacity = v; }

    public boolean isOccupied() { return activeSessionId != null; }
    public boolean isMerged() { return mergedIntoTableId != null; }
}
