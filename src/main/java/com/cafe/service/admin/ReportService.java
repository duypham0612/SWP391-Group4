package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.ReportDao;
import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 7 · ReportService — doanh thu toàn chuỗi (Admin, xem chéo chi nhánh). */
public class ReportService {

    private final ReportDao dao = new ReportDao();

    public ChainSummary getChainSummary() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.chainSummary(c); }
    }

    public List<ReportRow> getRevenueByBranch() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.revenueByBranch(c); }
    }

    public List<ReportRow> getTopProducts(int top) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.topProducts(c, top); }
    }

    public List<ReportRow> getPaymentBreakdown() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.paymentBreakdown(c); }
    }

    // ===== Bản LỌC THEO KHOẢNG NGÀY (Dashboard: filter + biểu đồ + export) =====

    public ChainSummary getChainSummary(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.chainSummary(c, from, to); }
    }

    public List<ReportRow> getRevenueByBranch(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.revenueByBranch(c, from, to); }
    }

    public List<ReportRow> getTopProducts(int top, LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.topProducts(c, top, from, to); }
    }

    public List<ReportRow> getPaymentBreakdown(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.paymentBreakdown(c, from, to); }
    }

    /** Chuỗi doanh thu theo ngày, ĐÃ bù ngày trống = 0 (cho biểu đồ liên tục). */
    public List<ReportRow> getDailyRevenue(LocalDate from, LocalDate to) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Map<String, ReportRow> got = new LinkedHashMap<>();
            for (ReportRow r : dao.dailyRevenue(c, from, to)) got.put(r.getLabel(), r);
            List<ReportRow> out = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                ReportRow r = got.get(d.toString());   // yyyy-MM-dd khớp CONVERT(...,23)
                out.add(r != null ? r : new ReportRow(d.toString(), 0, BigDecimal.ZERO));
            }
            return out;
        }
    }
}
