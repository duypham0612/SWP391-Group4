package com.mycoffee.controller.barista;

import com.mycoffee.dao.BaristaDAO;
import com.mycoffee.model.BaristaQueueItem;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Màn 5: Chi tiết 1 order - xem toàn bộ món và trạng thái trong đơn. */
@WebServlet(name = "BaristaOrderDetailController", urlPatterns = {"/barista-detail"})
public class BaristaOrderDetailController extends HttpServlet {

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
        int orderId;
        try { orderId = Integer.parseInt(request.getParameter("orderId")); }
        catch (Exception e) { orderId = -1; }

        List<BaristaQueueItem> items = dao.getOrderItems(orderId);
        int totalQty = 0;
        for (BaristaQueueItem it : items) totalQty += it.getQuantity();
        request.setAttribute("orderId", orderId);
        request.setAttribute("orderItems", items);
        request.setAttribute("totalQty", totalQty);
        request.getRequestDispatcher("/views/barista/barista_detail.jsp").forward(request, response);
    }
}
