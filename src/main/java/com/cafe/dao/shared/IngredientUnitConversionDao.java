package com.cafe.dao.shared;

import com.cafe.model.IngredientUnitConversion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IngredientUnitConversionDao {
    private static final String SELECT = "SELECT IngredientUnitConversionId,IngredientId,UnitName,FactorToBase,"
            + "IsBaseUnit,IsActive,CreatedAt,UpdatedAt,UpdatedBy FROM catalog.IngredientUnitConversion ";

    public List<IngredientUnitConversion> findByIngredient(Connection c,int ingredientId,boolean activeOnly)throws SQLException{
        String sql=SELECT+"WHERE IngredientId=?"+(activeOnly?" AND IsActive=1":"")+" ORDER BY IsBaseUnit DESC,UnitName";
        List<IngredientUnitConversion> out=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,ingredientId);try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}}
        return out;
    }

    public List<IngredientUnitConversion> findAllActive(Connection c)throws SQLException{
        List<IngredientUnitConversion> out=new ArrayList<>();
        try(PreparedStatement ps=c.prepareStatement(SELECT+"WHERE IsActive=1 ORDER BY IngredientId,IsBaseUnit DESC,UnitName");ResultSet rs=ps.executeQuery()){
            while(rs.next())out.add(map(rs));
        }
        return out;
    }

    public IngredientUnitConversion findForUse(Connection c,int conversionId,int ingredientId)throws SQLException{
        String sql=SELECT+"WITH (UPDLOCK,HOLDLOCK) WHERE IngredientUnitConversionId=? AND IngredientId=? AND IsActive=1";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,conversionId);ps.setInt(2,ingredientId);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}
    }

    public IngredientUnitConversion findBaseForUse(Connection c,int ingredientId)throws SQLException{
        String sql=SELECT+"WITH (UPDLOCK,HOLDLOCK) WHERE IngredientId=? AND IsBaseUnit=1 AND IsActive=1";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,ingredientId);try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}
    }

    public int insertBase(Connection c,int ingredientId,String unitName,Integer userId)throws SQLException{
        return insert(c,ingredientId,unitName,java.math.BigDecimal.ONE,true,true,userId);
    }

    public int insert(Connection c,int ingredientId,String unitName,java.math.BigDecimal factor,boolean base,
                      boolean active,Integer userId)throws SQLException{
        String sql="INSERT catalog.IngredientUnitConversion(IngredientId,UnitName,FactorToBase,IsBaseUnit,IsActive,UpdatedBy) VALUES(?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setInt(1,ingredientId);ps.setString(2,unitName);ps.setBigDecimal(3,factor);ps.setBoolean(4,base);ps.setBoolean(5,active);
            if(userId==null)ps.setNull(6,Types.INTEGER);else ps.setInt(6,userId);ps.executeUpdate();
            try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getInt(1):0;}
        }
    }

    public int update(Connection c,int id,int ingredientId,String unitName,java.math.BigDecimal factor,
                      boolean active,int userId)throws SQLException{
        String sql="UPDATE catalog.IngredientUnitConversion SET UnitName=?,FactorToBase=?,IsActive=?,UpdatedBy=?,UpdatedAt=SYSUTCDATETIME() "
                + "WHERE IngredientUnitConversionId=? AND IngredientId=? AND IsBaseUnit=0";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,unitName);ps.setBigDecimal(2,factor);ps.setBoolean(3,active);ps.setInt(4,userId);ps.setInt(5,id);ps.setInt(6,ingredientId);return ps.executeUpdate();}
    }

    public int deactivate(Connection c,int id,int ingredientId,int userId)throws SQLException{
        try(PreparedStatement ps=c.prepareStatement("UPDATE catalog.IngredientUnitConversion SET IsActive=0,UpdatedBy=?,UpdatedAt=SYSUTCDATETIME() WHERE IngredientUnitConversionId=? AND IngredientId=? AND IsBaseUnit=0")){
            ps.setInt(1,userId);ps.setInt(2,id);ps.setInt(3,ingredientId);return ps.executeUpdate();
        }
    }

    public int renameBase(Connection c,int ingredientId,String unitName,Integer userId)throws SQLException{
        String sql="UPDATE catalog.IngredientUnitConversion SET UnitName=?,UpdatedBy=?,UpdatedAt=SYSUTCDATETIME() WHERE IngredientId=? AND IsBaseUnit=1";
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,unitName);if(userId==null)ps.setNull(2,Types.INTEGER);else ps.setInt(2,userId);ps.setInt(3,ingredientId);return ps.executeUpdate();}
    }

    private IngredientUnitConversion map(ResultSet rs)throws SQLException{
        IngredientUnitConversion x=new IngredientUnitConversion();x.setIngredientUnitConversionId(rs.getInt("IngredientUnitConversionId"));x.setIngredientId(rs.getInt("IngredientId"));x.setUnitName(rs.getString("UnitName"));x.setFactorToBase(rs.getBigDecimal("FactorToBase"));x.setBaseUnit(rs.getBoolean("IsBaseUnit"));x.setActive(rs.getBoolean("IsActive"));Timestamp c=rs.getTimestamp("CreatedAt");if(c!=null)x.setCreatedAt(c.toLocalDateTime());Timestamp u=rs.getTimestamp("UpdatedAt");if(u!=null)x.setUpdatedAt(u.toLocalDateTime());int by=rs.getInt("UpdatedBy");x.setUpdatedBy(rs.wasNull()?null:by);return x;
    }
}
