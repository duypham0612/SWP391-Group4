package com.mycoffee.controller.cashier; // Cập nhật đúng package của bạn

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.dao.ProductDAO;
import com.mycoffee.dao.VoucherDAO; // BẮT BUỘC PHẢI IMPORT THÊM DÒNG NÀY
import com.mycoffee.model.Order;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.Product;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "PosOrderController", urlPatterns = {"/pos"})
public class PosOrderController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || (user.getRoleId() > 3)) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        OrderDAO orderDAO = new OrderDAO();
        int branchId = 1;

        if ("create".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            int orderId = orderDAO.createNewOrder(branchId, tableId, user.getUserId());
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("open_table".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            int existingOrderId = orderDAO.getPendingOrderIdByTable(tableId);

            if (existingOrderId > 0) {
                response.sendRedirect("pos?action=view&orderId=" + existingOrderId);
            } else {
                int newOrderId = orderDAO.createNewOrder(branchId, tableId, user.getUserId());
                response.sendRedirect("pos?action=view&orderId=" + newOrderId);
            }

        } else if ("cancel_order".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            orderDAO.cancelOrder(orderId, tableId);
            response.sendRedirect("pos-tables");

        } else if ("reset_all".equals(action)) {
            orderDAO.resetAllTables(branchId);
            response.sendRedirect("pos-tables");

        } else if ("add_item".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int productId = Integer.parseInt(request.getParameter("productId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            double price = Double.parseDouble(request.getParameter("price"));

            orderDAO.addOrUpdateOrderDetail(orderId, productId, quantity, price);
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("apply_voucher".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String voucherCode = request.getParameter("voucherCode");

            orderDAO.applyVoucher(orderId, voucherCode);
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("view".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));

            Order order = orderDAO.getOrderById(orderId);
            if(order == null) {
                response.sendRedirect("pos-tables");
                return;
            }

            // 1. Đẩy dữ liệu cơ bản
            request.setAttribute("order", order);
            request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            request.setAttribute("products", new ProductDAO().getAllAvailableProducts());

            // 2. ĐẨY DỮ LIỆU VOUCHER SANG JSP (ĐÂY LÀ DÒNG CODE QUAN TRỌNG NHẤT BỊ THIẾU)
            request.setAttribute("availableVouchers", new VoucherDAO().getValidVouchersForOrder(orderId));

            request.getRequestDispatcher("pos_screen.jsp").forward(request, response);

        } else {
            response.sendRedirect("pos-tables");
        }
    }
}