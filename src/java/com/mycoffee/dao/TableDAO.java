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
        return getTablesForCustomerCheckout(branchId, 0);
    }

    public List<Table> getTablesForCustomerCheckout(int branchId, int selectedTableId) {
        List<Table> list = new ArrayList<>();
        String sql = "SELECT TableID, BranchID, TableName, QRCodeURL, Status "
                + "FROM Tables "
                + "WHERE BranchID = ? "
                + "AND ((Status IS NULL OR Status IN ('Empty', N'Trống')) "
                + "     OR (TableID = ? AND Status = 'Selected')) "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM Orders o "
                + "    WHERE o.TableID = Tables.TableID "
                + "    AND o.OrderStatus IN ('Pending', 'Preparing', N'Đang xử lý')"
                + ") "
                + "ORDER BY TableID";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, branchId);
            ps.setInt(2, selectedTableId);
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

    public boolean selectTableForCustomer(int branchId, int tableId) {
        String sql = "UPDATE Tables SET Status = 'Selected' "
                + "WHERE TableID = ? AND BranchID = ? "
                + "AND (Status IS NULL OR Status IN ('Empty', N'Trống')) "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM Orders o "
                + "    WHERE o.TableID = Tables.TableID "
                + "    AND o.OrderStatus IN ('Pending', 'Preparing', N'Đang xử lý')"
                + ")";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, branchId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            System.out.println("Loi tai selectTableForCustomer (TableDAO): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean releaseCustomerSelectedTable(int tableId) {
        String sql = "UPDATE Tables SET Status = 'Empty' "
                + "WHERE TableID = ? AND Status = 'Selected' "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM Orders o "
                + "    WHERE o.TableID = Tables.TableID "
                + "    AND o.OrderStatus IN ('Pending', 'Preparing', N'Đang xử lý')"
                + ")";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            System.out.println("Loi tai releaseCustomerSelectedTable (TableDAO): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
