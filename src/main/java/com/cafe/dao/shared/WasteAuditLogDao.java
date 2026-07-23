package com.cafe.dao.shared;

import java.sql.*;

public class WasteAuditLogDao {
    public void insert(Connection c,Integer logId,Long eventId,String action,String before,String after,String reason,int userId)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory.WasteAuditLog(WasteLogId,WasteEventId,ActionType,BeforeValue,AfterValue,Reason,PerformedBy) VALUES (?,?,?,?,?,?,?)")){
            if(logId==null)ps.setNull(1,Types.INTEGER);else ps.setInt(1,logId);if(eventId==null)ps.setNull(2,Types.BIGINT);else ps.setLong(2,eventId);ps.setString(3,action);text(ps,4,before);text(ps,5,after);text(ps,6,reason);ps.setInt(7,userId);ps.executeUpdate();
        }
    }
    private static void text(PreparedStatement ps,int i,String v)throws SQLException{if(v==null||v.isBlank())ps.setNull(i,Types.NVARCHAR);else ps.setString(i,v);}
}
