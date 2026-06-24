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

/** Màn 8: In tem ly / mã order cho 1 món. */
@WebServlet(name = "BaristaLabelController", urlPatterns = {"/barista-label"})
public class BaristaLabelController extends HttpServlet {

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
        int orderDetailId;
        try { orderDetailId = Integer.parseInt(request.getParameter("orderDetailId")); }
        catch (Exception e) { orderDetailId = -1; }

        request.setAttribute("labelItem", dao.getLabelItem(orderDetailId));
        request.getRequestDispatcher("/views/barista/barista_label.jsp").forward(request, response);
    }
}
