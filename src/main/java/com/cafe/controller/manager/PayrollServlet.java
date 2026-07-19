package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.Payroll;
import com.cafe.model.PayrollRow;
import com.cafe.model.User;
import com.cafe.service.manager.PayrollService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** M4 · PayrollServlet → /manager/payroll. show | save (chốt giờ+lương/giờ) | export (CSV/Excel). */
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
            List<PayrollRow> rows = service.getMonthlyPayroll(branchId, monthStart, ym.toString());
            if ("export".equals(req.getParameter("action"))) {
                exportCsv(resp, ym, rows);
                return;
            }
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("rows", rows);
            req.setAttribute("minHourlyRate", Constants.MIN_HOURLY_RATE);
            req.setAttribute("pageTitle", "Bảng lương");
            req.getRequestDispatcher("/WEB-INF/views/manager/payroll.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        YearMonth ym = parseMonth(req.getParameter("month"));
        try {
            String[] uids = req.getParameterValues("uid");
            List<Payroll> lines = new ArrayList<>();
            if (uids != null) for (String s : uids) {
                int uid;
                try { uid = Integer.parseInt(s); } catch (NumberFormatException e) { continue; }
                Payroll p = new Payroll();
                p.setUserId(uid);
                p.setWorkedHours(dec(req.getParameter("hours_" + uid)));
                p.setHourlyRate(dec(req.getParameter("rate_" + uid)));
                lines.add(p);
            }
            service.savePayroll(branchId, ym.toString(), lines, u != null ? u.getUserId() : 0);
            req.getSession().setAttribute("flashOk", "Đã lưu bảng lương tháng " + ym + ".");
            resp.sendRedirect(req.getContextPath() + "/manager/payroll?month=" + ym);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/payroll?month=" + ym);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Giờ làm hoặc lương/giờ không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/manager/payroll?month=" + ym);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Xuất CSV (mở được bằng Excel) — BOM UTF-8 để hiển thị tiếng Việt đúng. */
    private void exportCsv(HttpServletResponse resp, YearMonth ym, List<PayrollRow> rows) throws IOException {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"payroll-" + ym + ".csv\"");
        PrintWriter w = resp.getWriter();
        w.write('﻿');   // BOM
        w.println("Mã NV,Họ tên,Vai trò,Số ca duyệt,Tổng giờ,Lương/giờ,Thành tiền");
        for (PayrollRow r : rows) {
            w.println(r.getUserId() + "," + csv(r.getUserName()) + "," + csv(r.getRoleName())
                    + "," + r.getApprovedShifts() + "," + String.format(java.util.Locale.US, "%.2f", r.getTotalHours())
                    + "," + r.getHourlyRate().toPlainString() + "," + r.getSalary().toPlainString());
        }
        w.flush();
    }

    private BigDecimal dec(String s) {
        return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
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
