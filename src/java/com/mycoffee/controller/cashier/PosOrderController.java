package com.mycoffee.controller.cashier;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.dao.ProductDAO;
import com.mycoffee.model.Order;
import com.mycoffee.model.User;
import com.mycoffee.service.OrderService;
import com.mycoffee.service.VoucherService;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "PosOrderController", urlPatterns = {"/pos"})
public class PosOrderController extends HttpServlet {

    // KHAI BÁO SERVICE LÀM NHIỆM VỤ XỬ LÝ NGHIỆP VỤ
    private final OrderService orderService = new OrderService();
    private final VoucherService voucherService = new VoucherService();
    private final OrderDAO orderDAO = new OrderDAO(); // DAO giờ chỉ dùng để đọc (view) dữ liệu

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
        int branchId = 1;

        if ("open_table".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            // Đã đổi sang dùng orderService
            int orderId = orderService.openOrCreateTableOrder(branchId, tableId, user.getUserId());
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("cancel_order".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            orderService.cancelTable(orderId, tableId);
            response.sendRedirect("pos-tables");

            // THÊM MỚI NHÁNH NÀY: Bắt sự kiện Hủy bàn trực tiếp từ sơ đồ
        } else if ("cancel_by_table".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            orderService.cancelOrderByTableId(tableId);
            response.sendRedirect("pos-tables");

        } else if ("reset_all".equals(action)) {
            // Đã đổi sang dùng orderService
            orderService.resetBranchTables(branchId);
            response.sendRedirect("pos-tables");

        } else if ("add_item".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int productId = Integer.parseInt(request.getParameter("productId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            double price = Double.parseDouble(request.getParameter("price"));

            // Đã đổi sang dùng orderService
            orderService.addOrUpdateItem(orderId, productId, quantity, price);
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("apply_voucher".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String voucherCode = request.getParameter("voucherCode");

            // Đã đổi sang dùng orderService
            orderService.applyVoucher(orderId, voucherCode);
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("checkout".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath());

            // Gọi Service lo liệu việc chuẩn hóa link PayOS
            String checkoutUrl = orderService.generatePaymentLink(orderId, baseUrl);
            if (checkoutUrl != null) {
                response.sendRedirect(checkoutUrl);
                return;
            }
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("payos_return".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String status = request.getParameter("status");

            if ("PAID".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                orderService.completeOrder(orderId);
                response.sendRedirect("pos?action=receipt&orderId=" + orderId);
            } else {
                response.sendRedirect("pos?action=view&orderId=" + orderId);
            }

        } else if ("receipt".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            request.setAttribute("order", orderDAO.getOrderById(orderId));
            request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            request.getRequestDispatcher("receipt.jsp").forward(request, response);

        } else if ("view".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            Order order = orderDAO.getOrderById(orderId);
            if (order == null) {
                response.sendRedirect("pos-tables");
                return;
            }
            request.setAttribute("order", order);
            request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            request.setAttribute("products", new ProductDAO().getAllAvailableProducts());

            // Gọi qua tầng Service để lấy danh sách Voucher hợp lệ
            request.setAttribute("availableVouchers", voucherService.getValidVouchersForOrder(orderId));

            request.getRequestDispatcher("pos_screen.jsp").forward(request, response);
        } else {
            response.sendRedirect("pos-tables");
        }
    }
}