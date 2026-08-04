package com.cafe.dao.barista;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderItemWorkflowDao {

    // EN: Atomic UPDATE, only succeeds if item is still WAITING in an ACTIVE order of this branch. Returns rows affected (0/1).
    public int claim(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='MAKING',oi.BaristaId=?,oi.StartedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='WAITING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId); ps.setInt(2, orderItemId); ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    // EN: Atomic UPDATE, only succeeds if item is MAKING and owned by this barista. Returns rows affected (0/1); caller only deducts stock when this returns 1.
    public int completeClaimed(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='READY',oi.DoneAt=SYSUTCDATETIME(),oi.PreparedBy=?,"
                + "oi.HasIssue=0,oi.IssueReason=NULL,oi.RemakeInventoryReserved=0 "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId);
            ps.setInt(2, orderItemId); ps.setInt(3, branchId); ps.setInt(4, baristaId);
            return ps.executeUpdate();
        }
    }

    public int countMakingByBarista(Connection conn, int branchId, int baristaId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, baristaId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    // EN: Atomic UPDATE, only succeeds if item is still MAKING and owned by expectedBaristaId (the old owner, not the caller). Returns rows affected (0/1).
    public int reclaim(Connection conn, int orderItemId, int branchId, int expectedBaristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' "
                + "  AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, expectedBaristaId);
            return ps.executeUpdate();
        }
    }

    // EN: Atomic UPDATE, only succeeds if item is MAKING and owned by this barista. Returns rows affected (0/1).
    public int returnToQueue(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, baristaId);
            return ps.executeUpdate();
        }
    }
}
