package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Table;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public List<Table> getTablesByBranch(int branchId) {
        List<Table> list = new ArrayList<>();
        String sql = "SELECT TableID, BranchID, TableName, QRCodeURL, Status, Capacity FROM Tables WHERE BranchID = ?";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Table table = new Table();
                    
                    table.setTableID(rs.getInt("TableID"));
                    table.setBranchID(rs.getInt("BranchID"));
                    table.setTableName(rs.getString("TableName"));
                    table.setQrCodeURL(rs.getString("QRCodeURL"));
                    table.setStatus(rs.getString("Status"));
                    table.setCapacity(rs.getInt("Capacity"));
                    
                    list.add(table);
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi tại getTablesByBranch (TableDAO): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<Table> getTablesForCustomerCheckout(int branchId) {
        List<Table> list = new ArrayList<>();
        String sql = "SELECT TableID, BranchID, TableName, QRCodeURL, Status "
                + "FROM Tables "
                + "WHERE BranchID = ? "
                + "AND (Status IS NULL OR Status IN ('Empty', N'Trống')) "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM Orders o "
                + "    WHERE o.TableID = Tables.TableID "
                + "    AND o.OrderStatus IN ('Pending', 'Preparing', N'Đang xử lý')"
                + ") "
                + "ORDER BY TableID";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Table table = new Table();
                    table.setTableID(rs.getInt("TableID"));
                    table.setBranchID(rs.getInt("BranchID"));
                    table.setTableName(rs.getString("TableName"));
                    table.setQrCodeURL(rs.getString("QRCodeURL"));
                    table.setStatus(rs.getString("Status"));
                    list.add(table);
                }
            }
        } catch (Exception e) {
            System.out.println("Loi tai getTablesForCustomerCheckout (TableDAO): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}
