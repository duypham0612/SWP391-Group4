package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** payment.CashierShift — ca thu ngân (mở/đóng quỹ). */
public class CashierShift {
    private int cashierShiftId;
    private int branchId;
    private int cashierId;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    // join / report
    private String cashierName;
    private int billCount;
    private BigDecimal totalCollected;

    public int getCashierShiftId() { return cashierShiftId; }
    public void setCashierShiftId(int v) { this.cashierShiftId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getCashierId() { return cashierId; }
    public void setCashierId(int v) { this.cashierId = v; }

    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal v) { this.openingCash = v; }

    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal v) { this.closingCash = v; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime v) { this.openedAt = v; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime v) { this.closedAt = v; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String v) { this.cashierName = v; }

    public int getBillCount() { return billCount; }
    public void setBillCount(int v) { this.billCount = v; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal v) { this.totalCollected = v; }

    public boolean isOpen() { return closedAt == null; }
}
