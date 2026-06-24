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

/** Màn 6: Lịch sử order - các món đã pha xong, lọc theo ngày. */
@WebServlet(name = "BaristaHistoryController", urlPatterns = {"/barista-history"})
public class BaristaHistoryController extends HttpServlet {

    private final BaristaDAO dao = new BaristaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() > 4) {
            response.sendRedirect("login");
            return;
        }
        Integer b = (Integer) session.getAttribute("branchId");
        int branchId = (b == null) ? 1 : b;

        String date = request.getParameter("date");
        if (date != null && date.trim().isEmpty()) date = null;
        request.setAttribute("historyList", dao.getHistory(branchId, date));
        request.setAttribute("selectedDate", date);
        request.getRequestDispatcher("/views/barista/barista_history.jsp").forward(request, response);
    }
}
