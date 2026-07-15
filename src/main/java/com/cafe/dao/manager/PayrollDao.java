package com.cafe.dao.manager;

import com.cafe.model.Payroll;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/** M4 · PayrollDao — chốt lương theo tháng (upsert theo UNIQUE(UserId, PayMonth)). */
public class PayrollDao {

    /** Map userId → Payroll đã chốt của tháng (để overlay lên giờ tính từ chấm công). */
    public Map<Integer, Payroll> findByMonth(Connection conn, int branchId, String payMonth) throws SQLException {
        Map<Integer, Payroll> out = new HashMap<>();
        final String sql = "SELECT PayrollId, BranchId, UserId, PayMonth, WorkedHours, HourlyRate " +
                "FROM hr.Payroll WHERE BranchId=? AND PayMonth=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, payMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payroll p = new Payroll();
                    p.setPayrollId(rs.getInt("PayrollId"));
                    p.setBranchId(rs.getInt("BranchId"));
                    p.setUserId(rs.getInt("UserId"));
                    p.setPayMonth(rs.getString("PayMonth"));
                    p.setWorkedHours(rs.getBigDecimal("WorkedHours"));
                    p.setHourlyRate(rs.getBigDecimal("HourlyRate"));
                    out.put(p.getUserId(), p);
                }
            }
        }
        return out;
    }

    /** Upsert 1 dòng lương (UPDATE trước, không có thì INSERT) — trong tx của caller. */
    public void upsert(Connection conn, int branchId, int userId, String payMonth,
                       BigDecimal workedHours, BigDecimal hourlyRate, Integer updatedBy) throws SQLException {
        final String upd = "UPDATE hr.Payroll SET WorkedHours=?, HourlyRate=?, BranchId=?, UpdatedBy=?, UpdatedAt=SYSUTCDATETIME() " +
                "WHERE UserId=? AND PayMonth=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setBigDecimal(1, workedHours);
            ps.setBigDecimal(2, hourlyRate);
            ps.setInt(3, branchId);
            if (updatedBy == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, updatedBy);
            ps.setInt(5, userId);
            ps.setString(6, payMonth);
            if (ps.executeUpdate() > 0) return;
        }
        final String ins = "INSERT INTO hr.Payroll(BranchId, UserId, PayMonth, WorkedHours, HourlyRate, UpdatedBy) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setInt(1, branchId);
            ps.setInt(2, userId);
            ps.setString(3, payMonth);
            ps.setBigDecimal(4, workedHours);
            ps.setBigDecimal(5, hourlyRate);
            if (updatedBy == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, updatedBy);
            ps.executeUpdate();
        }
    }
}
