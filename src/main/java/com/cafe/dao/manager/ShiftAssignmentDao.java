package com.cafe.dao.manager;

import com.cafe.model.ShiftAssignment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShiftAssignmentDao {

    /** Join template + user; lọc theo chi nhánh của template. */
    private static final String SELECT =
        "SELECT sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate, " +
        "       st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName, r.Code AS RoleCode " +
        "FROM hr.ShiftAssignment sa " +
        "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
        "JOIN iam.[User] u        ON u.UserId = sa.UserId " +
        "JOIN iam.Role r          ON r.RoleId = u.RoleId ";

    /** Lịch tuần: tất cả phân công của chi nhánh trong [weekStart, weekStart+7). */
    public List<ShiftAssignment> findByBranchAndWeek(Connection conn, int branchId, LocalDate weekStart) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE st.BranchId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? " +
            "ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setDate(3, Date.valueOf(weekStart.plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công trong khoảng ngày, dùng để tìm ca tiếp theo cho bàn giao. */
    public List<ShiftAssignment> findByBranchRange(Connection conn, int branchId, LocalDate from, LocalDate untilExclusive) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE st.BranchId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(untilExclusive));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công của 1 nhân viên trong 1 ngày — dùng cho detectConflict (kèm giờ ca). */
    public List<ShiftAssignment> findByUserAndDate(Connection conn, int userId, LocalDate date) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT + "WHERE sa.UserId=? AND sa.WorkDate=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công của 1 nhân viên trong tuần [weekStart, weekStart+7). */
    public List<ShiftAssignment> findByUserAndWeek(Connection conn, int userId, LocalDate weekStart) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE sa.UserId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? " +
            "ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setDate(3, Date.valueOf(weekStart.plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public ShiftAssignment findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE sa.ShiftAssignmentId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, int templateId, int userId, LocalDate workDate) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftAssignment(ShiftTemplateId, UserId, WorkDate) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, templateId);
            ps.setInt(2, userId);
            ps.setDate(3, Date.valueOf(workDate));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM hr.ShiftAssignment WHERE ShiftAssignmentId=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private ShiftAssignment map(ResultSet rs) throws SQLException {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        a.setShiftTemplateId(rs.getInt("ShiftTemplateId"));
        a.setUserId(rs.getInt("UserId"));
        Date d = rs.getDate("WorkDate");
        if (d != null) a.setWorkDate(d.toLocalDate());
        a.setTemplateName(rs.getString("TemplateName"));
        Time st = rs.getTime("StartTime");
        Time et = rs.getTime("EndTime");
        if (st != null) a.setStartTime(st.toLocalTime());
        if (et != null) a.setEndTime(et.toLocalTime());
        a.setUserName(rs.getString("UserName"));
        a.setRoleCode(rs.getString("RoleCode"));
        return a;
    }
}
