package com.cafe.dao.manager;

import com.cafe.model.Payroll;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/** M4 · PayrollDao — chốt lương riêng theo (BranchId, UserId, PayrollMonth). */
public class PayrollDao {

    /** Map userId → Payroll đã chốt của tháng (để overlay lên giờ tính từ chấm công). */
    public Map<Integer, Payroll> findByMonth(Connection conn, int branchId, YearMonth payrollMonth) throws SQLException {
        Map<Integer, Payroll> out = new HashMap<>();
        final String sql = "SELECT PayrollId, BranchId, UserId, PayrollMonth, WorkedHours, HourlyRate " +
                "FROM hr.Payroll WHERE BranchId=? AND PayrollMonth=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(payrollMonth.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payroll p = new Payroll();
                    p.setPayrollId(rs.getInt("PayrollId"));
                    p.setBranchId(rs.getInt("BranchId"));
                    p.setUserId(rs.getInt("UserId"));
                    p.setPayrollMonth(YearMonth.from(rs.getDate("PayrollMonth").toLocalDate()));
                    p.setWorkedHours(rs.getBigDecimal("WorkedHours"));
                    p.setHourlyRate(rs.getBigDecimal("HourlyRate"));
                    out.put(p.getUserId(), p);
                }
            }
        }
        return out;
    }

    /** Upsert 1 dòng lương (UPDATE trước, không có thì INSERT) — trong tx của caller. */
    public boolean upsert(Connection conn, int branchId, int userId, YearMonth payrollMonth,
                          BigDecimal workedHours, BigDecimal hourlyRate, Integer updatedBy) throws SQLException {
        final String upd = "UPDATE p SET WorkedHours=?, HourlyRate=?, UpdatedBy=?, UpdatedAt=SYSUTCDATETIME() " +
                "FROM hr.Payroll p " +
                "WHERE p.BranchId=? AND p.UserId=? AND p.PayrollMonth=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setBigDecimal(1, workedHours);
            ps.setBigDecimal(2, hourlyRate);
            if (updatedBy == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, updatedBy);
            ps.setInt(4, branchId);
            ps.setInt(5, userId);
            ps.setDate(6, Date.valueOf(payrollMonth.atDay(1)));
            if (ps.executeUpdate() == 1) return true;
        }
        final String ins = "INSERT INTO hr.Payroll(BranchId, UserId, PayrollMonth, WorkedHours, HourlyRate, UpdatedBy) " +
                "SELECT ?, u.UserId, ?, ?, ?, ? FROM iam.UserAccount u " +
                "WHERE u.UserId=? AND EXISTS(SELECT 1 FROM hr.Attendance a " +
                "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
                "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
                "WHERE sa.UserId=u.UserId AND st.BranchId=? AND a.Status='APPROVED' " +
                "AND sa.WorkDate>=? AND sa.WorkDate<?)";
        try (PreparedStatement ps = conn.prepareStatement(ins)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(payrollMonth.atDay(1)));
            ps.setBigDecimal(3, workedHours);
            ps.setBigDecimal(4, hourlyRate);
            if (updatedBy == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, updatedBy);
            ps.setInt(6, userId);
            ps.setInt(7, branchId);
            ps.setDate(8, Date.valueOf(payrollMonth.atDay(1)));
            ps.setDate(9, Date.valueOf(payrollMonth.plusMonths(1).atDay(1)));
            return ps.executeUpdate() == 1;
        }
    }
}
