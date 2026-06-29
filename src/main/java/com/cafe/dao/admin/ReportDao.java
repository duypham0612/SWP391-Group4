package com.cafe.dao.admin;

import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Báo cáo doanh thu toàn chuỗi (Phase 7) — chỉ tính bill đã thanh toán (Status='PAID'). */
public class ReportDao {

    public ChainSummary chainSummary(Connection conn) throws SQLException {
        ChainSummary s = new ChainSummary();
        final String sql =
            "SELECT COUNT(*) AS Bills, ISNULL(SUM(TotalAmount),0) AS Rev, ISNULL(SUM(DiscountAmount),0) AS Disc, " +
            "       ISNULL(SUM(VatAmount),0) AS Vat, " +
            "       ISNULL(SUM(CASE WHEN CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE) THEN TotalAmount ELSE 0 END),0) AS TodayRev, " +
            "       ISNULL(SUM(CASE WHEN CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE) THEN 1 ELSE 0 END),0) AS TodayBills " +
            "FROM payment.Bill WHERE Status='PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setPaidBills(rs.getInt("Bills"));
                s.setRevenue(rs.getBigDecimal("Rev"));
                s.setDiscount(rs.getBigDecimal("Disc"));
                s.setVat(rs.getBigDecimal("Vat"));
                s.setTodayRevenue(rs.getBigDecimal("TodayRev"));
                s.setTodayBills(rs.getInt("TodayBills"));
            }
        }
        return s;
    }

    public List<ReportRow> revenueByBranch(Connection conn) throws SQLException {
        final String sql =
            "SELECT br.Name AS Label, COUNT(b.BillId) AS Cnt, ISNULL(SUM(b.TotalAmount),0) AS Amt " +
            "FROM org.Branch br LEFT JOIN payment.Bill b ON b.BranchId=br.BranchId AND b.Status='PAID' " +
            "GROUP BY br.Name ORDER BY Amt DESC";
        return query(conn, sql);
    }

    public List<ReportRow> topProducts(Connection conn, int top) throws SQLException {
        final String sql =
            "SELECT TOP " + top + " p.Name AS Label, SUM(oi.Quantity) AS Cnt, ISNULL(SUM(bi.Amount),0) AS Amt " +
            "FROM payment.BillItem bi " +
            "JOIN payment.Bill b      ON b.BillId=bi.BillId AND b.Status='PAID' " +
            "JOIN sales.OrderItem oi  ON oi.OrderItemId=bi.OrderItemId " +
            "JOIN catalog.Product p   ON p.ProductId=oi.ProductId " +
            "GROUP BY p.Name ORDER BY Amt DESC";
        return query(conn, sql);
    }

    public List<ReportRow> paymentBreakdown(Connection conn) throws SQLException {
        final String sql =
            "SELECT ISNULL(PaymentMethod,'?') AS Label, COUNT(*) AS Cnt, ISNULL(SUM(TotalAmount),0) AS Amt " +
            "FROM payment.Bill WHERE Status='PAID' GROUP BY PaymentMethod ORDER BY Amt DESC";
        return query(conn, sql);
    }

    private List<ReportRow> query(Connection conn, String sql) throws SQLException {
        List<ReportRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new ReportRow(rs.getString("Label"), rs.getInt("Cnt"), rs.getBigDecimal("Amt")));
        }
        return out;
    }

    // ===== Bản LỌC THEO KHOẢNG NGÀY (from..to, inclusive) cho Dashboard =====

    /** Tổng hợp trong khoảng [from..to] + số liệu "hôm nay" (luôn theo ngày hiện tại). */
    public ChainSummary chainSummary(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        ChainSummary s = new ChainSummary();
        final String range =
            "SELECT COUNT(*) AS Bills, ISNULL(SUM(TotalAmount),0) AS Rev, ISNULL(SUM(DiscountAmount),0) AS Disc, " +
            "       ISNULL(SUM(VatAmount),0) AS Vat " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ?";
        try (PreparedStatement ps = conn.prepareStatement(range)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.setPaidBills(rs.getInt("Bills"));
                    s.setRevenue(rs.getBigDecimal("Rev"));
                    s.setDiscount(rs.getBigDecimal("Disc"));
                    s.setVat(rs.getBigDecimal("Vat"));
                }
            }
        }
        final String today =
            "SELECT ISNULL(SUM(TotalAmount),0) AS Rev, COUNT(*) AS Bills FROM payment.Bill " +
            "WHERE Status='PAID' AND CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE)";
        try (PreparedStatement ps = conn.prepareStatement(today); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) { s.setTodayRevenue(rs.getBigDecimal("Rev")); s.setTodayBills(rs.getInt("Bills")); }
        }
        return s;
    }

    public List<ReportRow> revenueByBranch(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        final String sql =
            "SELECT br.Name AS Label, COUNT(b.BillId) AS Cnt, ISNULL(SUM(b.TotalAmount),0) AS Amt " +
            "FROM org.Branch br LEFT JOIN payment.Bill b ON b.BranchId=br.BranchId AND b.Status='PAID' " +
            "  AND b.PaidAt >= ? AND b.PaidAt < ? " +
            "GROUP BY br.Name ORDER BY Amt DESC";
        return queryRange(conn, sql, from, to);
    }

    public List<ReportRow> topProducts(Connection conn, int top, LocalDate from, LocalDate to) throws SQLException {
        final String sql =
            "SELECT TOP " + top + " p.Name AS Label, SUM(oi.Quantity) AS Cnt, ISNULL(SUM(bi.Amount),0) AS Amt " +
            "FROM payment.BillItem bi " +
            "JOIN payment.Bill b      ON b.BillId=bi.BillId AND b.Status='PAID' AND b.PaidAt >= ? AND b.PaidAt < ? " +
            "JOIN sales.OrderItem oi  ON oi.OrderItemId=bi.OrderItemId " +
            "JOIN catalog.Product p   ON p.ProductId=oi.ProductId " +
            "GROUP BY p.Name ORDER BY Amt DESC";
        return queryRange(conn, sql, from, to);
    }

    public List<ReportRow> paymentBreakdown(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        final String sql =
            "SELECT ISNULL(PaymentMethod,'?') AS Label, COUNT(*) AS Cnt, ISNULL(SUM(TotalAmount),0) AS Amt " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ? GROUP BY PaymentMethod ORDER BY Amt DESC";
        return queryRange(conn, sql, from, to);
    }

    /** Doanh thu theo từng ngày (chỉ ngày có bán) — Service tự bù ngày trống cho biểu đồ. */
    public List<ReportRow> dailyRevenue(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        final String sql =
            "SELECT CONVERT(varchar(10), CAST(PaidAt AS DATE), 23) AS Label, COUNT(*) AS Cnt, ISNULL(SUM(TotalAmount),0) AS Amt " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ? " +
            "GROUP BY CAST(PaidAt AS DATE) ORDER BY CAST(PaidAt AS DATE)";
        return queryRange(conn, sql, from, to);
    }

    private List<ReportRow> queryRange(Connection conn, String sql, LocalDate from, LocalDate to) throws SQLException {
        List<ReportRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new ReportRow(rs.getString("Label"), rs.getInt("Cnt"), rs.getBigDecimal("Amt")));
            }
        }
        return out;
    }
}
