package com.cafe.dao.manager;

import com.cafe.model.ShiftTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ShiftTemplateDao {

    private static final String SELECT =
        "SELECT ShiftTemplateId, BranchId, Name, StartTime, EndTime FROM hr.ShiftTemplate ";

    public List<ShiftTemplate> findByBranch(Connection conn, int branchId) throws SQLException {
        List<ShiftTemplate> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE BranchId=? ORDER BY StartTime")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public ShiftTemplate findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE ShiftTemplateId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, ShiftTemplate t) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftTemplate(BranchId, Name, StartTime, EndTime) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getBranchId());
            ps.setString(2, t.getName());
            ps.setTime(3, Time.valueOf(t.getStartTime()));
            ps.setTime(4, Time.valueOf(t.getEndTime()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM hr.ShiftTemplate WHERE ShiftTemplateId=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private ShiftTemplate map(ResultSet rs) throws SQLException {
        ShiftTemplate t = new ShiftTemplate();
        t.setShiftTemplateId(rs.getInt("ShiftTemplateId"));
        t.setBranchId(rs.getInt("BranchId"));
        t.setName(rs.getString("Name"));
        Time st = rs.getTime("StartTime");
        Time et = rs.getTime("EndTime");
        if (st != null) t.setStartTime(st.toLocalTime());
        if (et != null) t.setEndTime(et.toLocalTime());
        return t;
    }
}
