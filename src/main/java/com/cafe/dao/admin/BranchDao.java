package com.cafe.dao.admin;

import com.cafe.model.Branch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BranchDao {

    private static final String SELECT =
        "SELECT b.BranchId, b.Code, b.Name, b.Address, b.Phone, b.IsActive, " +
        "       b.OpenTime, b.CloseTime, b.ManagerUserId, b.PeakThresholdCups, u.FullName AS ManagerName " +
        "FROM org.Branch b LEFT JOIN iam.[User] u ON u.UserId = b.ManagerUserId ";

    public List<Branch> findAll(Connection conn) throws SQLException {
        List<Branch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY b.Code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Branch> findAllActive(Connection conn) throws SQLException {
        List<Branch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE b.IsActive = 1 ORDER BY b.Code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public Branch findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE b.BranchId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Branch b) throws SQLException {
        final String sql = "INSERT INTO org.Branch(Code, Name, Address, Phone, OpenTime, CloseTime, ManagerUserId, IsActive) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "TMP" + Long.toString(System.nanoTime(), 36));
            ps.setString(2, b.getName());
            ps.setString(3, b.getAddress());
            ps.setString(4, b.getPhone());
            setTime(ps, 5, b.getOpenTime());
            setTime(ps, 6, b.getCloseTime());
            setInt(ps, 7, b.getManagerUserId());
            ps.setBoolean(8, b.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void updateCode(Connection conn, int id, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE org.Branch SET Code=? WHERE BranchId=?")) {
            ps.setString(1, code);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void update(Connection conn, Branch b) throws SQLException {
        // Không đụng PeakThresholdCups ở đây: cột đó do Manager quản qua updateHoursAndPeak,
        // để lưu chi nhánh từ màn Admin không vô tình xoá ngưỡng cao điểm về 0.
        final String sql = "UPDATE org.Branch SET Name=?, Address=?, Phone=?, " +
                "OpenTime=?, CloseTime=?, ManagerUserId=?, IsActive=? WHERE BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getName());
            ps.setString(2, b.getAddress());
            ps.setString(3, b.getPhone());
            setTime(ps, 4, b.getOpenTime());
            setTime(ps, 5, b.getCloseTime());
            setInt(ps, 6, b.getManagerUserId());
            ps.setBoolean(7, b.isActive());
            ps.setInt(8, b.getBranchId());
            ps.executeUpdate();
        }
    }

    private static void setTime(PreparedStatement ps, int idx, java.time.LocalTime t) throws SQLException {
        if (t == null) ps.setNull(idx, java.sql.Types.TIME); else ps.setTime(idx, java.sql.Time.valueOf(t));
    }

    private static void setInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.INTEGER); else ps.setInt(idx, v);
    }

    /** Cài đặt vận hành cho Manager: giờ mở/đóng cửa + ngưỡng cao điểm của chi nhánh mình. */
    public void updateHoursAndPeak(Connection conn, int branchId, java.time.LocalTime openTime,
                                   java.time.LocalTime closeTime, int peakThresholdCups) throws SQLException {
        final String sql = "UPDATE org.Branch SET OpenTime=?, CloseTime=?, PeakThresholdCups=? WHERE BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setTime(ps, 1, openTime);
            setTime(ps, 2, closeTime);
            ps.setInt(3, Math.max(0, peakThresholdCups));
            ps.setInt(4, branchId);
            ps.executeUpdate();
        }
    }

    public void updateActive(Connection conn, int id, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE org.Branch SET IsActive=? WHERE BranchId=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateManager(Connection conn, int branchId, Integer userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE org.Branch SET ManagerUserId=? WHERE BranchId=?")) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    private Branch map(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("BranchId"));
        b.setCode(rs.getString("Code"));
        b.setName(rs.getString("Name"));
        b.setAddress(rs.getString("Address"));
        b.setPhone(rs.getString("Phone"));
        b.setActive(rs.getBoolean("IsActive"));
        java.sql.Time ot = rs.getTime("OpenTime");
        java.sql.Time ct = rs.getTime("CloseTime");
        if (ot != null) b.setOpenTime(ot.toLocalTime());
        if (ct != null) b.setCloseTime(ct.toLocalTime());
        int mgr = rs.getInt("ManagerUserId");
        if (!rs.wasNull()) b.setManagerUserId(mgr);
        b.setPeakThresholdCups(rs.getInt("PeakThresholdCups"));
        b.setManagerName(rs.getString("ManagerName"));
        return b;
    }
}
