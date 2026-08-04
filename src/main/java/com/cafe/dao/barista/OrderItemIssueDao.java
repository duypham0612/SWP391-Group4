package com.cafe.dao.barista;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Issue handling for a single order item line: flagging, block/unblock, and remake cycle.
 *
 * <p>Three severity levels, read from lightest to heaviest and DO NOT mix them up:
 * <ul>
 *   <li>{@link #reportIssue} — only flags the item, its status STAYS THE SAME and it remains in
 *       the queue;</li>
 *   <li>{@link #blockItem} — the item LEAVES the queue for BLOCKED and releases its owner, so
 *       someone else doesn't click "Take" and run into the exact same problem;</li>
 *   <li>{@link #beginRemake} / {@link #finishRemake} — the item was already made but must be
 *       remade from scratch.</li>
 * </ul>
 *
 * <p>REMAKE is a TRANSITIONAL state that only exists between {@code beginRemake} and
 * {@code finishRemake} within the same transaction — other transactions never observe it. It
 * exists so two people can't create the same remake at once.
 */
public class OrderItemIssueDao {

    /** Flags the issue but keeps the status so the card doesn't disappear from whoever is handling it. */
    public int reportIssue(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.HasIssue=1,oi.IssueReason=?,oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status IN ('WAITING','MAKING') "
                + "AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /**
     * WAITING/MAKING → BLOCKED: item cannot be made (out of ingredients, equipment broken, item
     * discontinued). Releases the owner + start timestamp since the item left the making flow;
     * keeps the reason so it shows in the "Needs attention" area.
     * Same guard as reportIssue: if the item is being made, only its own owner can block it.
     */
    public int blockItem(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='BLOCKED',oi.HasIssue=1,oi.IssueReason=?,"
                + "oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME(),oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' "
                + "AND oi.Status IN ('WAITING','MAKING') AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /** BLOCKED → WAITING: ingredient/equipment is available again, return item to the queue and clear the issue flags. */
    public int unblockItem(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.HasIssue=0,oi.IssueReason=NULL,"
                + "oi.IssueReportedBy=NULL,oi.IssueReportedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='BLOCKED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    /** Counts remaining BLOCKED item lines in the branch that use one of the ingredients just stocktaken. */
    public int countBlockedUsingIngredients(Connection conn, int branchId,
                                            java.util.Collection<Integer> ingredientIds) throws SQLException {
        if (ingredientIds == null || ingredientIds.isEmpty()) return 0;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ingredientIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql =
            "SELECT COUNT(DISTINCT oi.OrderItemId) " +
            "FROM sales.OrderItem oi " +
            "JOIN sales.SalesOrder o ON o.OrderId = oi.OrderId " +
            "JOIN catalog.Recipe pr ON pr.OwnerType='PRODUCT' AND pr.OwnerId=oi.ProductId " +
            "WHERE o.BranchId = ? AND o.Status = 'ACTIVE' AND oi.Status = 'BLOCKED' " +
            "AND pr.IngredientId IN (" + in + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            for (Integer ingredientId : ingredientIds) ps.setInt(idx++, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /** READY → REMAKE is a transitional claim, prevents two people from creating a duplicate remake. */
    public int beginRemake(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='REMAKE' FROM sales.OrderItem oi "
                + "JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='READY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); return ps.executeUpdate();
        }
    }

    /** MAKING → REMAKE only the barista currently holding the item can report it as wrong/needing a remake. */
    public int beginRemakeClaimed(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='REMAKE' FROM sales.OrderItem oi "
                + "JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, baristaId); return ps.executeUpdate();
        }
    }

    /**
     * REMAKE → WAITING with remake priority. {@code inventoryReserved} decides whether the next
     * "Done" click deducts stock again or not — the rule lives in
     * {@link com.cafe.common.RemakeReservation}.
     */
    public void finishRemake(Connection conn, int orderItemId, int branchId, boolean inventoryReserved)
            throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.Priority=(SELECT ISNULL(MAX(x.Priority),0)+1 "
                + "FROM sales.OrderItem x JOIN sales.SalesOrder xo ON xo.OrderId=x.OrderId WHERE xo.BranchId=?),"
                + "oi.RemakeCount=oi.RemakeCount+1,oi.RemakeInventoryReserved=?,oi.BaristaId=NULL,oi.PreparedBy=NULL,"
                + "oi.StartedAt=NULL,oi.DoneAt=NULL,oi.HasIssue=0,oi.IssueReason=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='REMAKE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setBoolean(2, inventoryReserved);
            ps.setInt(3, orderItemId); ps.setInt(4, branchId); ps.executeUpdate();
        }
    }
}
