package com.cafe.dao.shared;

import com.cafe.model.WasteEventReview;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class WasteEventReviewDao {
    public void insert(Connection c,long eventId,int ingredientId,String type,BigDecimal before,BigDecimal after,String note)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory.WasteEventReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter,Note) VALUES (?,?,?,?,?,?)")){
            ps.setLong(1,eventId);ps.setInt(2,ingredientId);ps.setString(3,type);ps.setBigDecimal(4,before);ps.setBigDecimal(5,after);if(note==null)ps.setNull(6,Types.NVARCHAR);else ps.setString(6,note);ps.executeUpdate();
        }
    }
    public List<WasteEventReview> findOpenByBranch(Connection c,int branchId)throws SQLException{
        String sql="SELECT r.*,i.Name IngredientName FROM inventory.WasteEventReview r JOIN inventory.WasteEvent e ON e.WasteEventId=r.WasteEventId JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId WHERE e.BranchId=? AND r.Status='OPEN' ORDER BY r.CreatedAt DESC";
        List<WasteEventReview> out=new ArrayList<>();try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,branchId);try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}}return out;
    }
    /** Resolve atomically and return the owning event id; null means already resolved/not in branch. */
    public Long resolveReturningEventId(Connection c,int branchId,long id,int managerId,String status,String note)throws SQLException{
        // SQL Server cấm OUTPUT không-INTO khi target có trigger. Table variable vẫn giữ
        // update có điều kiện và event id trong cùng một server batch/round-trip.
        String sql="DECLARE @resolved TABLE(WasteEventId BIGINT); "
                + "UPDATE r SET Status=?,ResolvedBy=?,ResolvedAt=SYSUTCDATETIME(),ResolutionNote=? "
                + "OUTPUT inserted.WasteEventId INTO @resolved(WasteEventId) "
                + "FROM inventory.WasteEventReview r JOIN inventory.WasteEvent e ON e.WasteEventId=r.WasteEventId "
                + "WHERE r.WasteEventReviewId=? AND e.BranchId=? AND r.Status='OPEN'; "
                + "SELECT WasteEventId FROM @resolved;";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,status);ps.setInt(2,managerId);if(note==null||note.isBlank())ps.setNull(3,Types.NVARCHAR);else ps.setString(3,note);ps.setLong(4,id);ps.setInt(5,branchId);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getLong(1):null;}
        }
    }
    private WasteEventReview map(ResultSet rs)throws SQLException{WasteEventReview r=new WasteEventReview();r.setWasteEventReviewId(rs.getLong("WasteEventReviewId"));r.setWasteEventId(rs.getLong("WasteEventId"));r.setIngredientId(rs.getInt("IngredientId"));r.setIngredientName(rs.getString("IngredientName"));r.setReviewType(rs.getString("ReviewType"));r.setQtyBefore(rs.getBigDecimal("QtyBefore"));r.setQtyAfter(rs.getBigDecimal("QtyAfter"));r.setStatus(rs.getString("Status"));r.setNote(rs.getString("Note"));Timestamp t=rs.getTimestamp("CreatedAt");if(t!=null)r.setCreatedAt(t.toLocalDateTime());return r;}
}
