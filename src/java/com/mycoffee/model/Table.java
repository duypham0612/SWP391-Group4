package com.mycoffee.model;

public class Table {
    private int tableId;
    private int branchId;
    private String tableName;
    private String qrCodeUrl;
    private String status;

    public Table() {
    }

    public Table(int tableId, int branchId, String tableName, String qrCodeUrl, String status) {
        this.tableId = tableId;
        this.branchId = branchId;
        this.tableName = tableName;
        this.qrCodeUrl = qrCodeUrl;
        this.status = status;
    }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}