package com.mycoffee.model;

public class Table {

    private int tableID;
    private Integer branchID;
    private String tableName;
    private String qrCodeURL;
    private String status;
    private int capacity;

    public Table() {
    }

    public Table(int tableID, Integer branchID, String tableName, String qrCodeURL, String status, int capacity) {
        this.tableID = tableID;
        this.branchID = branchID;
        this.tableName = tableName;
        this.qrCodeURL = qrCodeURL;
        this.status = status;
        this.capacity = capacity; 
    }

    public int getTableID() {
        return tableID;
    }

    public void setTableID(int tableID) {
        this.tableID = tableID;
    }

    public Integer getBranchID() {
        return branchID;
    }

    public void setBranchID(Integer branchID) {
        this.branchID = branchID;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getQrCodeURL() {
        return qrCodeURL;
    }

    public void setQrCodeURL(String qrCodeURL) {
        this.qrCodeURL = qrCodeURL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}