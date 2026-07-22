package com.cafe.dao.shared;

import com.cafe.model.WasteEvent;
import java.sql.*;

/** DAO event cấp nghiệp vụ cho hao hụt/remake. */
public class WasteEventDao {
    public long insert(Connection c, WasteEvent e) throws SQLException {
        String sql="INSERT INTO inventory.WasteEvent(BranchId,EventKind,Source,ProductId,OrderItemId,CupQuantity,CauseCode,CauseDetail,ShiftAssignmentId,CreatedBy,ClientRequestId) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,e.getBranchId()); ps.setString(2,e.getEventKind()); ps.setString(3,e.getSource());
            nullableInt(ps,4,e.getProductId()); nullableInt(ps,5,e.getOrderItemId()); nullableInt(ps,6,e.getCupQuantity());
            ps.setString(7,e.getCauseCode()); nullableString(ps,8,e.getCauseDetail()); nullableInt(ps,9,e.getShiftAssignmentId());
            ps.setInt(10,e.getCreatedBy()); nullableString(ps,11,e.getClientRequestId()); ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getLong(1):0;}
        }
    }
    public Long findIdByClientRequest(Connection c,int branchId,String requestId)throws SQLException{
        if(requestId==null||requestId.isBlank())return null;
        try(PreparedStatement ps=c.prepareStatement("SELECT WasteEventId FROM inventory.WasteEvent WHERE BranchId=? AND ClientRequestId=?")){
            ps.setInt(1,branchId);ps.setString(2,requestId);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getLong(1):null;}
        }
    }
    public WasteEvent findById(Connection c,long id)throws SQLException{
        String sql="SELECT e.*,p.Name ProductName FROM inventory.WasteEvent e LEFT JOIN catalog.Product p ON p.ProductId=e.ProductId WHERE e.WasteEventId=?";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}
    }
    /** Đồng bộ nguyên nhân event khi Barista sửa một dòng hao hụt thủ công của chính mình. */
    public int updateCause(Connection c, long eventId, String causeCode, String causeDetail) throws SQLException {
        String sql = "UPDATE inventory.WasteEvent SET CauseCode=?, CauseDetail=? "
                + "WHERE WasteEventId=? AND EventKind='INGREDIENT_WASTE' AND Source='MANUAL'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, causeCode);
            nullableString(ps, 2, causeDetail);
            ps.setLong(3, eventId);
            return ps.executeUpdate();
        }
    }
    private WasteEvent map(ResultSet rs)throws SQLException{
        WasteEvent e=new WasteEvent(); e.setWasteEventId(rs.getLong("WasteEventId"));e.setBranchId(rs.getInt("BranchId"));e.setEventKind(rs.getString("EventKind"));e.setSource(rs.getString("Source"));
        e.setProductId(nullable(rs,"ProductId"));e.setOrderItemId(nullable(rs,"OrderItemId"));e.setCupQuantity(nullable(rs,"CupQuantity"));e.setCauseCode(rs.getString("CauseCode"));e.setCauseDetail(rs.getString("CauseDetail"));e.setShiftAssignmentId(nullable(rs,"ShiftAssignmentId"));e.setCreatedBy(rs.getInt("CreatedBy"));
        Timestamp t=rs.getTimestamp("CreatedAt");if(t!=null)e.setCreatedAt(t.toLocalDateTime());e.setClientRequestId(rs.getString("ClientRequestId"));e.setProductName(rs.getString("ProductName"));return e;
    }
    private static Integer nullable(ResultSet rs,String col)throws SQLException{int v=rs.getInt(col);return rs.wasNull()?null:v;}
    private static void nullableInt(PreparedStatement ps,int i,Integer v)throws SQLException{if(v==null)ps.setNull(i,Types.INTEGER);else ps.setInt(i,v);}
    private static void nullableString(PreparedStatement ps,int i,String v)throws SQLException{if(v==null||v.isBlank())ps.setNull(i,Types.NVARCHAR);else ps.setString(i,v);}
}
