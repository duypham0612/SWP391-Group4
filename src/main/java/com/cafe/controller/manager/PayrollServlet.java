package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.model.Payroll;
import com.cafe.model.PayrollRow;
import com.cafe.model.User;
import com.cafe.service.manager.PayrollService;
import com.cafe.web.renderer.PayrollCsvRenderer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** M4 · PayrollServlet → /manager/payroll. show | save (chốt giờ+lương/giờ) | export (CSV/Excel). */
@WebServlet("/manager/payroll")
public class PayrollServlet extends HttpServlet {

    private final PayrollService service;
    private final PayrollCsvRenderer csvRenderer;

    public PayrollServlet() {
        this(new PayrollService(), new PayrollCsvRenderer());
    }

    PayrollServlet(PayrollService service, PayrollCsvRenderer csvRenderer) {
        this.service = Objects.requireNonNull(service, "service");
        this.csvRenderer = Objects.requireNonNull(csvRenderer, "csvRenderer");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        YearMonth ym = parseMonth(req.getParameter("month"));
        try {
            List<PayrollRow> rows = service.getMonthlyPayroll(branchId, ym);
            if ("export".equals(req.getParameter("action"))) {
                csvRenderer.render(resp, ym, rows);
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
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
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
            service.savePayroll(branchId, ym, lines, u != null ? u.getUserId() : 0);
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

    private BigDecimal dec(String s) {
        return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
    }

    private YearMonth parseMonth(String month) {
        try { return (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month); }
        catch (DateTimeParseException e) { return YearMonth.now(); }
    }
}
