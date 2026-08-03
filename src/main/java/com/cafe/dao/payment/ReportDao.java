package com.cafe.dao.payment;

import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Báo cáo doanh thu toàn chuỗi (Phase 7) — chỉ tính bill đã thanh toán (Status='PAID'). */
public class ReportDao {

    public ChainSummary chainSummary(Connection conn, LocalDateTime todayFromUtc,
                                     LocalDateTime todayToUtc) throws SQLException {
        ChainSummary s = new ChainSummary();
        final String sql =
            "SELECT COUNT(*) AS Bills, ISNULL(SUM(TotalAmount),0) AS Rev, ISNULL(SUM(DiscountAmount),0) AS Disc, " +
            "       ISNULL(SUM(VatAmount),0) AS Vat, " +
            "       ISNULL(SUM(CASE WHEN PaidAt>=? AND PaidAt<? THEN TotalAmount ELSE 0 END),0) AS TodayRev, " +
            "       ISNULL(SUM(CASE WHEN PaidAt>=? AND PaidAt<? THEN 1 ELSE 0 END),0) AS TodayBills " +
            "FROM payment.Bill WHERE Status='PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(todayFromUtc));
            ps.setTimestamp(2, Timestamp.valueOf(todayToUtc));
            ps.setTimestamp(3, Timestamp.valueOf(todayFromUtc));
            ps.setTimestamp(4, Timestamp.valueOf(todayToUtc));
            try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setPaidBills(rs.getInt("Bills"));
                s.setRevenue(rs.getBigDecimal("Rev"));
                s.setDiscount(rs.getBigDecimal("Disc"));
                s.setVat(rs.getBigDecimal("Vat"));
                s.setTodayRevenue(rs.getBigDecimal("TodayRev"));
                s.setTodayBills(rs.getInt("TodayBills"));
            }
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
            "SELECT TOP " + top + " oi.ProductNameAtOrder AS Label,SUM(oi.Quantity) AS Cnt,ISNULL(SUM(oi.BilledAmount),0) AS Amt " +
            "FROM sales.OrderItem oi " +
            "JOIN payment.Bill b ON b.BillId=oi.BillId AND b.Status='PAID' " +
            "GROUP BY oi.ProductNameAtOrder ORDER BY Amt DESC";
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
    public ChainSummary chainSummary(Connection conn, LocalDateTime fromUtc, LocalDateTime toUtc,
                                     LocalDateTime todayFromUtc, LocalDateTime todayToUtc) throws SQLException {
        ChainSummary s = new ChainSummary();
        final String range =
            "SELECT COUNT(*) AS Bills, ISNULL(SUM(TotalAmount),0) AS Rev, ISNULL(SUM(DiscountAmount),0) AS Disc, " +
            "       ISNULL(SUM(VatAmount),0) AS Vat " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ?";
        try (PreparedStatement ps = conn.prepareStatement(range)) {
            ps.setTimestamp(1, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(2, Timestamp.valueOf(toUtc));
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
            "WHERE Status='PAID' AND PaidAt>=? AND PaidAt<?";
        try (PreparedStatement ps = conn.prepareStatement(today)) {
            ps.setTimestamp(1, Timestamp.valueOf(todayFromUtc));
            ps.setTimestamp(2, Timestamp.valueOf(todayToUtc));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { s.setTodayRevenue(rs.getBigDecimal("Rev")); s.setTodayBills(rs.getInt("Bills")); }
            }
        }
        return s;
    }

    public List<ReportRow> revenueByBranch(Connection conn, LocalDateTime fromUtc,
                                           LocalDateTime toUtc) throws SQLException {
        final String sql =
            "SELECT br.Name AS Label, COUNT(b.BillId) AS Cnt, ISNULL(SUM(b.TotalAmount),0) AS Amt " +
            "FROM org.Branch br LEFT JOIN payment.Bill b ON b.BranchId=br.BranchId AND b.Status='PAID' " +
            "  AND b.PaidAt >= ? AND b.PaidAt < ? " +
            "GROUP BY br.Name ORDER BY Amt DESC";
        return queryRange(conn, sql, fromUtc, toUtc);
    }

    public List<ReportRow> topProducts(Connection conn, int top, LocalDateTime fromUtc,
                                       LocalDateTime toUtc) throws SQLException {
        final String sql =
            "SELECT TOP " + top + " oi.ProductNameAtOrder AS Label,SUM(oi.Quantity) AS Cnt,ISNULL(SUM(oi.BilledAmount),0) AS Amt " +
            "FROM sales.OrderItem oi " +
            "JOIN payment.Bill b ON b.BillId=oi.BillId AND b.Status='PAID' AND b.PaidAt>=? AND b.PaidAt<? " +
            "GROUP BY oi.ProductNameAtOrder ORDER BY Amt DESC";
        return queryRange(conn, sql, fromUtc, toUtc);
    }

    public List<ReportRow> paymentBreakdown(Connection conn, LocalDateTime fromUtc,
                                            LocalDateTime toUtc) throws SQLException {
        final String sql =
            "SELECT ISNULL(PaymentMethod,'?') AS Label, COUNT(*) AS Cnt, ISNULL(SUM(TotalAmount),0) AS Amt " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ? GROUP BY PaymentMethod ORDER BY Amt DESC";
        return queryRange(conn, sql, fromUtc, toUtc);
    }

    /** Doanh thu theo từng ngày (chỉ ngày có bán) — Service tự bù ngày trống cho biểu đồ. */
    public List<ReportRow> dailyRevenue(Connection conn, LocalDateTime fromUtc,
                                        LocalDateTime toUtc) throws SQLException {
        // Việt Nam luôn UTC+7 và không có DST. DATEADD tránh phụ thuộc SQL Server CLR
        // của AT TIME ZONE (SQL Edge/minimal instance có thể tắt CLR); WHERE vẫn lọc
        // trực tiếp trên PaidAt bằng half-open UTC range để giữ SARGability.
        final String vnDate = "CONVERT(date,DATEADD(hour,7,PaidAt))";
        final String sql =
            "SELECT CONVERT(varchar(10)," + vnDate + ",23) AS Label, COUNT(*) AS Cnt, ISNULL(SUM(TotalAmount),0) AS Amt " +
            "FROM payment.Bill WHERE Status='PAID' AND PaidAt >= ? AND PaidAt < ? " +
            "GROUP BY " + vnDate + " ORDER BY " + vnDate;
        return queryRange(conn, sql, fromUtc, toUtc);
    }

    private List<ReportRow> queryRange(Connection conn, String sql, LocalDateTime fromUtc,
                                       LocalDateTime toUtc) throws SQLException {
        List<ReportRow> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(2, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new ReportRow(rs.getString("Label"), rs.getInt("Cnt"), rs.getBigDecimal("Amt")));
            }
        }
        return out;
    }
}
