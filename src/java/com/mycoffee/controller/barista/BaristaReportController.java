package com.mycoffee.controller.barista;

import com.mycoffee.dao.BaristaDAO;
import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Màn 7: Báo cáo thống kê pha chế (số ly + thời gian pha TB theo món, lọc khoảng ngày). */
@WebServlet(name = "BaristaReportController", urlPatterns = {"/barista-report"})
public class BaristaReportController extends HttpServlet {

    private final BaristaDAO dao = new BaristaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() != 4) {
            response.sendRedirect("login");
            return;
        }
        Integer b = (Integer) session.getAttribute("branchId");
        int branchId = (b == null) ? 1 : b;

        String from = request.getParameter("from");
        String to = request.getParameter("to");
        if (from != null && from.trim().isEmpty()) from = null;
        if (to != null && to.trim().isEmpty()) to = null;

        request.setAttribute("statList", dao.getProductStats(branchId, from, to));
        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.getRequestDispatcher("/views/barista/barista_report.jsp").forward(request, response);
    }
}
