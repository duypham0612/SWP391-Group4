package com.mycoffee.controller.customer;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.dao.TableDAO;
import com.mycoffee.model.CartItem;
import com.mycoffee.model.Table;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CustomerCheckoutController", urlPatterns = {"/customer-checkout"})
public class CustomerCheckoutController extends HttpServlet {

    private static final int DEFAULT_BRANCH_ID = 1;
    private final OrderDAO orderDAO = new OrderDAO();
    private final TableDAO tableDAO = new TableDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        List<CartItem> cart = getCart(session);

        int tableId = parseInt(request.getParameter("tableId"), 0);
        String note = request.getParameter("note");
        User user = (User) session.getAttribute("user");
        Integer customerId = (user != null && user.getRoleId() == 5) ? user.getUserId() : null;
        boolean qrVerified = session.getAttribute("customerQrVerified") instanceof Boolean
                && (Boolean) session.getAttribute("customerQrVerified");
        Object savedTableId = session.getAttribute("customerTableId");

        if (!qrVerified || !(savedTableId instanceof Integer) || ((Integer) savedTableId) != tableId || tableId <= 0) {
            session.setAttribute("cartError", "Vui lòng quét QR tại bàn bằng camera trước khi gửi order.");
            response.sendRedirect(request.getContextPath() + "/customer-qr-order");
            return;
        }

        if (!isTableAvailable(tableId)) {
            session.removeAttribute("customerTableId");
            session.removeAttribute("customerQrVerified");
            session.setAttribute("cartError", "Bàn này hiện không còn trống. Vui lòng quét bàn khác.");
            response.sendRedirect(request.getContextPath() + "/customer-qr-order");
            return;
        }

        if (cart.isEmpty()) {
            session.setAttribute("cartError", "Giỏ hàng đang trống, vui lòng thêm món trước.");
            response.sendRedirect(request.getContextPath() + "/customer-qr-order");
            return;
        }

        int orderId = orderDAO.createCustomerOrder(DEFAULT_BRANCH_ID, tableId, customerId, cart, note);
        if (orderId > 0) {
            cart.clear();
            session.setAttribute("cart", cart);
            session.setAttribute("lastCustomerOrderId", orderId);
            session.removeAttribute("customerQrVerified");
            session.removeAttribute("customerTableId");
            session.setAttribute("cartMessage", "Gửi order thành công! Mã đơn của bạn là #" + orderId + ".");
            response.sendRedirect(request.getContextPath() + "/customer-order-status?orderId=" + orderId);
        } else {
            session.setAttribute("cartError", "Không thể gửi order. Vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/customer-qr-order");
        }
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCart(HttpSession session) {
        Object data = session.getAttribute("cart");
        if (data instanceof List<?>) {
            return (List<CartItem>) data;
        }
        List<CartItem> cart = new ArrayList<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    private boolean isTableAvailable(int tableId) {
        List<Table> availableTables = tableDAO.getTablesForCustomerCheckout(DEFAULT_BRANCH_ID);
        for (Table table : availableTables) {
            if (table.getTableID() == tableId) {
                return true;
            }
        }
        return false;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
