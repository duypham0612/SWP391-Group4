package com.cafe.dao.shared;

import com.cafe.model.WasteAuditEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WasteAuditLogDao {
    public void insert(Connection c,Integer logId,Long eventId,String action,String before,String after,String reason,int userId)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory.WasteAuditLog(WasteLogId,WasteEventId,ActionType,BeforeValue,AfterValue,Reason,PerformedBy) VALUES (?,?,?,?,?,?,?)")){
            if(logId==null)ps.setNull(1,Types.INTEGER);else ps.setInt(1,logId);if(eventId==null)ps.setNull(2,Types.BIGINT);else ps.setLong(2,eventId);ps.setString(3,action);text(ps,4,before);text(ps,5,after);text(ps,6,reason);ps.setInt(7,userId);ps.executeUpdate();
        }
    }

    /**
     * Thao tác SỬA/HUỶ trên dòng hao hụt của một chi nhánh, mới nhất trước.
     * Bỏ CREATE vì lần ghi đầu đã nằm ở bảng nhật ký chính — ở đây Quản lý chỉ cần truy vết đính chính.
     * Chi nhánh lấy theo WasteLog (JOIN, không LEFT JOIN) để không lọt dòng của chi nhánh khác.
     */
    public List<WasteAuditEntry> findCorrectionsByBranchBetween(Connection c, int branchId,
                                                                LocalDateTime fromUtc, LocalDateTime toUtc,
                                                                int limit) throws SQLException {
        final String sql = "SELECT a.WasteAuditLogId,a.WasteLogId,a.WasteEventId,a.ActionType,a.BeforeValue,a.AfterValue,"
                + "       a.Reason,a.PerformedBy,a.PerformedAt,u.FullName AS PerformedByName,"
                + "       i.Name AS IngredientName,i.Unit AS IngredientUnit,wl.WasteType "
                + "FROM inventory.WasteAuditLog a "
                + "JOIN inventory.WasteLog wl ON wl.WasteLogId=a.WasteLogId "
                + "JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId "
                + "JOIN iam.[User] u ON u.UserId=a.PerformedBy "
                + "WHERE wl.BranchId=? AND a.ActionType<>'CREATE' AND a.PerformedAt>=? AND a.PerformedAt<? "
                + "ORDER BY a.PerformedAt DESC, a.WasteAuditLogId DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<WasteAuditEntry> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            ps.setInt(4, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private static WasteAuditEntry map(ResultSet rs) throws SQLException {
        WasteAuditEntry e = new WasteAuditEntry();
        e.setWasteAuditLogId(rs.getLong("WasteAuditLogId"));
        int logId = rs.getInt("WasteLogId"); if (!rs.wasNull()) e.setWasteLogId(logId);
        long eventId = rs.getLong("WasteEventId"); if (!rs.wasNull()) e.setWasteEventId(eventId);
        e.setActionType(rs.getString("ActionType"));
        e.setBeforeValue(rs.getString("BeforeValue"));
        e.setAfterValue(rs.getString("AfterValue"));
        e.setReason(rs.getString("Reason"));
        e.setPerformedBy(rs.getInt("PerformedBy"));
        Timestamp t = rs.getTimestamp("PerformedAt"); if (t != null) e.setPerformedAt(t.toLocalDateTime());
        e.setPerformedByName(rs.getString("PerformedByName"));
        e.setIngredientName(rs.getString("IngredientName"));
        e.setIngredientUnit(rs.getString("IngredientUnit"));
        e.setWasteType(rs.getString("WasteType"));
        return e;
    }

    private static void text(PreparedStatement ps,int i,String v)throws SQLException{if(v==null||v.isBlank())ps.setNull(i,Types.NVARCHAR);else ps.setString(i,v);}
}
