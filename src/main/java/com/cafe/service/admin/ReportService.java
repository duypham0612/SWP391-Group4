package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.ReportDao;
import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
}
