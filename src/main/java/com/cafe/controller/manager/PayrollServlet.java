package com.cafe.controller.manager;

import com.cafe.service.manager.PayrollService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** M4 · PayrollServlet → /manager/payroll. show. */
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
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("rows", service.getMonthlyPayroll(branchId, monthStart));
            req.setAttribute("pageTitle", "Bảng lương");
            req.getRequestDispatcher("/WEB-INF/views/manager/payroll.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private YearMonth parseMonth(String month) {
        try { return (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month); }
        catch (DateTimeParseException e) { return YearMonth.now(); }
    }
}
