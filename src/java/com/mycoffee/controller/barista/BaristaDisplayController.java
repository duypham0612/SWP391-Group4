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

/** Màn 3: Màn hình gọi món (Pickup display) - hiển thị các đơn đã pha xong cho khách xem. */
@WebServlet(name = "BaristaDisplayController", urlPatterns = {"/barista-display"})
public class BaristaDisplayController extends HttpServlet {

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

        request.setAttribute("readyList", dao.getReadyForPickup(branchId));
        request.getRequestDispatcher("/views/barista/barista_display.jsp").forward(request, response);
    }
}
