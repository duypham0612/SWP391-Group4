package com.cafe.model;

import java.time.LocalDateTime;

/** sales.TableSession — phiên bàn (xương sống). */
public class TableSession {
    private int tableSessionId;
    private int branchId;
    private int diningTableId;
    private Integer openedBy;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String status;             // OPEN | BILLED | CLOSED

    // join
    private String tableNumber;

    public int getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(int v) { this.tableSessionId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getDiningTableId() { return diningTableId; }
    public void setDiningTableId(int v) { this.diningTableId = v; }

    public Integer getOpenedBy() { return openedBy; }
    public void setOpenedBy(Integer v) { this.openedBy = v; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime v) { this.openedAt = v; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime v) { this.closedAt = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }
}
