package com.cafe.dao.shared;

import com.cafe.model.WasteReview;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class WasteReviewDao {
    public void insert(Connection c,long eventId,int ingredientId,String type,BigDecimal before,BigDecimal after,String note)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("INSERT INTO inventory.WasteReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter,Note) VALUES (?,?,?,?,?,?)")){
            ps.setLong(1,eventId);ps.setInt(2,ingredientId);ps.setString(3,type);ps.setBigDecimal(4,before);ps.setBigDecimal(5,after);if(note==null)ps.setNull(6,Types.NVARCHAR);else ps.setString(6,note);ps.executeUpdate();
        }
    }
    public List<WasteReview> findOpenByBranch(Connection c,int branchId)throws SQLException{
        String sql="SELECT r.*,i.Name IngredientName FROM inventory.WasteReview r JOIN inventory.WasteEvent e ON e.WasteEventId=r.WasteEventId JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId WHERE e.BranchId=? AND r.Status IN ('OPEN','ACKNOWLEDGED') ORDER BY r.CreatedAt DESC";
        List<WasteReview> out=new ArrayList<>();try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,branchId);try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}}return out;
    }
    public boolean resolve(Connection c,int branchId,long id,int managerId,String status,String note)throws SQLException{
        String sql="UPDATE r SET Status=?,ResolvedBy=?,ResolvedAt=SYSUTCDATETIME(),ResolutionNote=? FROM inventory.WasteReview r JOIN inventory.WasteEvent e ON e.WasteEventId=r.WasteEventId WHERE r.WasteReviewId=? AND e.BranchId=? AND r.Status IN ('OPEN','ACKNOWLEDGED')";
        try(PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,status);ps.setInt(2,managerId);if(note==null||note.isBlank())ps.setNull(3,Types.NVARCHAR);else ps.setString(3,note);ps.setLong(4,id);ps.setInt(5,branchId);return ps.executeUpdate()==1;
        }
    }
    private WasteReview map(ResultSet rs)throws SQLException{WasteReview r=new WasteReview();r.setWasteReviewId(rs.getLong("WasteReviewId"));r.setWasteEventId(rs.getLong("WasteEventId"));r.setIngredientId(rs.getInt("IngredientId"));r.setIngredientName(rs.getString("IngredientName"));r.setReviewType(rs.getString("ReviewType"));r.setQtyBefore(rs.getBigDecimal("QtyBefore"));r.setQtyAfter(rs.getBigDecimal("QtyAfter"));r.setStatus(rs.getString("Status"));r.setNote(rs.getString("Note"));Timestamp t=rs.getTimestamp("CreatedAt");if(t!=null)r.setCreatedAt(t.toLocalDateTime());return r;}
}
