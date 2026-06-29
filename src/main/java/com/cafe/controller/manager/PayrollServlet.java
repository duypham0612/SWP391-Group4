package com.cafe.controller.manager;

import com.cafe.model.PayrollRow;
import com.cafe.service.manager.PayrollService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/** M4 · PayrollServlet → /manager/payroll. show | export (CSV/Excel). */
@WebServlet("/manager/payroll")
public class PayrollServlet extends HttpServlet {

    private final PayrollService service = new PayrollService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        YearMonth ym = parseMonth(req.getParameter("month"));
        LocalDate monthStart = ym.atDay(1);
        try {
            if ("export".equals(req.getParameter("action"))) {
                exportCsv(resp, ym, service.getMonthlyPayroll(branchId, monthStart));
                return;
            }
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("rows", service.getMonthlyPayroll(branchId, monthStart));
            req.setAttribute("pageTitle", "Bảng lương");
            req.getRequestDispatcher("/WEB-INF/views/manager/payroll.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Xuất CSV (mở được bằng Excel) — BOM UTF-8 để hiển thị tiếng Việt đúng. */
    private void exportCsv(HttpServletResponse resp, YearMonth ym, List<PayrollRow> rows) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"payroll-" + ym + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write('﻿');   // BOM
        w.println("Mã NV,Họ tên,Vai trò,Số ca duyệt,Tổng giờ");
        for (PayrollRow r : rows) {
            w.println(r.getUserId() + "," + csv(r.getUserName()) + "," + csv(r.getRoleName())
                    + "," + r.getApprovedShifts() + "," + String.format(java.util.Locale.US, "%.2f", r.getTotalHours()));
        }
        w.flush();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private YearMonth parseMonth(String month) {
        try { return (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month); }
        catch (DateTimeParseException e) { return YearMonth.now(); }
    }
}
