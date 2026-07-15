package com.cafe.dao.manager;

import com.cafe.model.ShiftHandover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** hr.ShiftHandover DAO (B7). */
public class ShiftHandoverDao {

    public int insert(Connection conn, int branchId, String note, int createdBy) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandover(BranchId, Note, CreatedBy) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setString(2, note);
            ps.setInt(3, createdBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public List<ShiftHandover> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT sh.ShiftHandoverId, sh.BranchId, sh.Note, sh.CreatedBy, sh.CreatedAt, u.FullName AS CreatedByName " +
            "FROM hr.ShiftHandover sh JOIN iam.[User] u ON u.UserId=sh.CreatedBy " +
            "WHERE sh.BranchId=? ORDER BY sh.CreatedAt DESC";
        List<ShiftHandover> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShiftHandover s = new ShiftHandover();
                    s.setShiftHandoverId(rs.getInt("ShiftHandoverId"));
                    s.setBranchId(rs.getInt("BranchId"));
                    s.setNote(rs.getString("Note"));
                    s.setCreatedBy(rs.getInt("CreatedBy"));
                    Timestamp ca = rs.getTimestamp("CreatedAt");
                    if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
                    s.setCreatedByName(rs.getString("CreatedByName"));
                    out.add(s);
                }
            }
        }
        return out;
    }
}
