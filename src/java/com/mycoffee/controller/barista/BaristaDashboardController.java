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

/** Màn 1: Dashboard tổng quan pha chế (màn mặc định khi Barista đăng nhập). */
@WebServlet(name = "BaristaDashboardController", urlPatterns = {"/barista"})
public class BaristaDashboardController extends HttpServlet {

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

        int[] counts = dao.getDashboardCounts(branchId);
        request.setAttribute("countPending", counts[0]);
        request.setAttribute("countPreparing", counts[1]);
        request.setAttribute("countCompleted", counts[2]);
        int avg = dao.getAvgPrepSecondsToday(branchId);
        request.setAttribute("avgPrepLabel", (avg / 60) + "p " + (avg % 60) + "s");
        request.setAttribute("topProducts", dao.getProductStats(branchId, null, null));
        request.getRequestDispatcher("/views/barista/barista_dashboard.jsp").forward(request, response);
    }
}
