package com.cafe.controller.manager;

import com.cafe.model.PayrollRow;
import com.cafe.service.manager.PayrollService;
import com.cafe.web.renderer.PayrollCsvRenderer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/** Bảng lương runtime và xuất CSV; không còn thao tác chốt/sửa tay. */
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
        int branchId =
                com.cafe.web.support.BranchContext.requireBranchId(req);
        YearMonth month = parseMonth(req.getParameter("month"));
        try {
            List<PayrollRow> rows =
                    service.getMonthlyPayroll(branchId, month);
            if ("export".equals(req.getParameter("action"))) {
                csvRenderer.render(resp, month, rows);
                return;
            }
            req.setAttribute("month", month.toString());
            req.setAttribute("prevMonth", month.minusMonths(1));
            req.setAttribute("nextMonth", month.plusMonths(1));
            req.setAttribute("rows", rows);
            req.setAttribute("pageTitle", "Bảng lương");
            req.getRequestDispatcher(
                    "/WEB-INF/views/manager/payroll.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private YearMonth parseMonth(String value) {
        try {
            return value == null || value.isBlank()
                    ? YearMonth.now() : YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            return YearMonth.now();
        }
    }
}
