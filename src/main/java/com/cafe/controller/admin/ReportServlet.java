package com.cafe.controller.admin;

import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;
import com.cafe.service.admin.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Doanh thu toàn chuỗi đã MERGE vào Dashboard (/dashboard).
 * Servlet này giữ lại làm endpoint XUẤT EXCEL (CSV) theo khoảng ngày,
 * và redirect truy cập xem thường về /dashboard.
 */
@WebServlet("/admin/report")
public class ReportServlet extends HttpServlet {

    private final ReportService service = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!"export".equals(req.getParameter("action"))) {
            // đã gộp vào bảng điều khiển — giữ tương thích link/bookmark cũ
            String qs = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            resp.sendRedirect(req.getContextPath() + "/dashboard" + qs);
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate to = parseDate(req.getParameter("to"), today);
        LocalDate from = parseDate(req.getParameter("from"), to.minusDays(29));
        if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }
        try {
            exportCsv(resp, from, to,
                    service.getChainSummary(from, to),
                    service.getRevenueByBranch(from, to),
                    service.getPaymentBreakdown(from, to),
                    service.getTopProducts(20, from, to),
                    service.getDailyRevenue(from, to));
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Xuất CSV (mở bằng Excel) — BOM UTF-8 để tiếng Việt hiển thị đúng. */
    private void exportCsv(HttpServletResponse resp, LocalDate from, LocalDate to,
                           ChainSummary sum, List<ReportRow> byBranch, List<ReportRow> byMethod,
                           List<ReportRow> top, List<ReportRow> daily) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"doanh-thu_" + from + "_" + to + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write('﻿');   // BOM

        w.println("BÁO CÁO DOANH THU TOÀN CHUỖI");
        w.println("Từ ngày," + from + ",Đến ngày," + to);
        w.println();
        w.println("TỔNG HỢP KỲ");
        w.println("Doanh thu," + money(sum.getRevenue()));
        w.println("Số hoá đơn," + sum.getPaidBills());
        w.println("Tổng giảm giá," + money(sum.getDiscount()));
        w.println("Tổng VAT," + money(sum.getVat()));
        w.println();
        w.println("DOANH THU THEO CHI NHÁNH");
        w.println("Chi nhánh,Số HĐ,Doanh thu");
        for (ReportRow r : byBranch) w.println(csv(r.getLabel()) + "," + r.getCount() + "," + money(r.getAmount()));
        w.println();
        w.println("THEO HÌNH THỨC THANH TOÁN");
        w.println("Hình thức,Số HĐ,Doanh thu");
        for (ReportRow r : byMethod) w.println(csv(method(r.getLabel())) + "," + r.getCount() + "," + money(r.getAmount()));
        w.println();
        w.println("TOP SẢN PHẨM");
        w.println("Sản phẩm,Số lượng,Doanh thu");
        for (ReportRow r : top) w.println(csv(r.getLabel()) + "," + r.getCount() + "," + money(r.getAmount()));
        w.println();
        w.println("DOANH THU THEO NGÀY");
        w.println("Ngày,Số HĐ,Doanh thu");
        for (ReportRow r : daily) w.println(r.getLabel() + "," + r.getCount() + "," + money(r.getAmount()));
        w.flush();
    }

    private String money(BigDecimal v) { return v == null ? "0" : v.toBigInteger().toString(); }
    private String method(String m) {
        if ("CASH".equals(m)) return "Tiền mặt";
        if ("TRANSFER".equals(m)) return "Chuyển khoản";
        if ("QR_BANK".equals(m)) return "QR ngân hàng";
        return m == null ? "?" : m;
    }
    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
    private LocalDate parseDate(String s, LocalDate fb) {
        if (s == null || s.isBlank()) return fb;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return fb; }
    }
}
