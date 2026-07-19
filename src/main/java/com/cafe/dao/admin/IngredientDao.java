package com.cafe.dao.admin;

import com.cafe.model.Ingredient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class IngredientDao {

    public List<Ingredient> findAll(Connection conn) throws SQLException {
        final String sql = "SELECT IngredientId, Name, Unit, IngredientType, IsActive " +
                "FROM catalog.Ingredient ORDER BY IngredientType, Name";
        List<Ingredient> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Ingredient> findByType(Connection conn, String type) throws SQLException {
        final String sql = "SELECT IngredientId, Name, Unit, IngredientType, IsActive " +
                "FROM catalog.Ingredient WHERE IngredientType = ? AND IsActive = 1 ORDER BY Name";
        List<Ingredient> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public Ingredient findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT IngredientId, Name, Unit, IngredientType, IsActive " +
                "FROM catalog.Ingredient WHERE IngredientId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Ingredient i) throws SQLException {
        final String sql = "INSERT INTO catalog.Ingredient(Name, Unit, IngredientType, IsActive) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, i.getName());
            ps.setString(2, i.getUnit());
            ps.setString(3, i.getIngredientType());
            ps.setBoolean(4, i.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Ingredient i) throws SQLException {
        final String sql = "UPDATE catalog.Ingredient SET Name=?, Unit=?, IngredientType=?, IsActive=? WHERE IngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getName());
            ps.setString(2, i.getUnit());
            ps.setString(3, i.getIngredientType());
            ps.setBoolean(4, i.isActive());
            ps.setInt(5, i.getIngredientId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE catalog.Ingredient SET IsActive=0 WHERE IngredientId=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Ingredient map(ResultSet rs) throws SQLException {
        Ingredient i = new Ingredient();
        i.setIngredientId(rs.getInt("IngredientId"));
        i.setName(rs.getString("Name"));
        i.setUnit(rs.getString("Unit"));
        i.setIngredientType(rs.getString("IngredientType"));
        i.setActive(rs.getBoolean("IsActive"));
        return i;
    }
}
