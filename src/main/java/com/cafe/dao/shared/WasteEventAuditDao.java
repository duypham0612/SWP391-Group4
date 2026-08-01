package com.cafe.dao.shared;

import com.cafe.model.WasteEventAudit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WasteEventAuditDao {
    public void insert(Connection c,Integer logId,Long eventId,String action,String before,String after,String reason,int userId)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory.WasteEventAudit(WasteEventItemId,WasteEventId,ActionType,BeforeValue,AfterValue,Reason,PerformedBy) VALUES (?,?,?,?,?,?,?)")){
            if(logId==null)ps.setNull(1,Types.INTEGER);else ps.setInt(1,logId);if(eventId==null)ps.setNull(2,Types.BIGINT);else ps.setLong(2,eventId);ps.setString(3,action);text(ps,4,before);text(ps,5,after);text(ps,6,reason);ps.setInt(7,userId);ps.executeUpdate();
        }
    }

    /**
     * Thao tác SỬA/HUỶ trên dòng hao hụt của một chi nhánh, mới nhất trước.
     * Bỏ CREATE vì lần ghi đầu đã nằm ở bảng nhật ký chính — ở đây Quản lý chỉ cần truy vết đính chính.
     * Audit cấp event (ví dụ REVIEW) không có WasteEventItemId, nên scope chi nhánh được lấy
     * từ item nếu có, nếu không thì từ WasteEvent. Cả hai nhánh đều được kiểm bằng JOIN/FK.
     */
    public List<WasteEventAudit> findCorrectionsByBranchBetween(Connection c, int branchId,
                                                                LocalDateTime fromUtc, LocalDateTime toUtc,
                                                                int limit) throws SQLException {
        final String sql = "SELECT a.WasteEventAuditId,a.WasteEventItemId,a.WasteEventId,a.ActionType,a.BeforeValue,a.AfterValue,"
                + "       a.Reason,a.PerformedBy,a.PerformedAt,u.FullName AS PerformedByName,"
                + "       i.Name AS IngredientName,i.Unit AS IngredientUnit,wl.WasteType "
                + "FROM inventory.WasteEventAudit a "
                + "LEFT JOIN inventory.WasteEventItem wl ON wl.WasteEventItemId=a.WasteEventItemId "
                + "LEFT JOIN inventory.WasteEvent e ON e.WasteEventId=COALESCE(a.WasteEventId,wl.WasteEventId) "
                + "LEFT JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId "
                + "JOIN iam.UserAccount u ON u.UserId=a.PerformedBy "
                + "WHERE COALESCE(wl.BranchId,e.BranchId)=? AND a.ActionType<>'CREATE' "
                + "AND a.PerformedAt>=? AND a.PerformedAt<? "
                + "ORDER BY a.PerformedAt DESC, a.WasteEventAuditId DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<WasteEventAudit> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            ps.setInt(4, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private static WasteEventAudit map(ResultSet rs) throws SQLException {
        WasteEventAudit e = new WasteEventAudit();
        e.setWasteEventAuditId(rs.getLong("WasteEventAuditId"));
        int logId = rs.getInt("WasteEventItemId"); if (!rs.wasNull()) e.setWasteEventItemId(logId);
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
