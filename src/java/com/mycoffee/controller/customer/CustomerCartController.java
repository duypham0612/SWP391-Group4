package com.mycoffee.controller.customer;

import com.mycoffee.dao.ProductDAO;
import com.mycoffee.model.CartItem;
import com.mycoffee.model.Product;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CustomerCartController", urlPatterns = {"/customer-cart"})
public class CustomerCartController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        if (action == null) {
            action = "add";
        }

        HttpSession session = request.getSession();
        List<CartItem> cart = getCart(session);
        String message = null;
        String error = null;

        try {
            int productId = parseInt(request.getParameter("productId"), 0);
            int quantity = Math.max(1, parseInt(request.getParameter("quantity"), 1));

            if ("add".equals(action)) {
                Product product = productDAO.getAvailableProductById(productId);
                if (product == null) {
                    error = "Sản phẩm không tồn tại hoặc đang tạm ngừng bán.";
                } else {
                    addToCart(cart, product, quantity);
                    message = "Đã thêm món vào giỏ hàng.";
                }
            } else if ("increase".equals(action)) {
                changeQuantity(cart, productId, 1);
            } else if ("decrease".equals(action)) {
                changeQuantity(cart, productId, -1);
            } else if ("remove".equals(action)) {
                removeFromCart(cart, productId);
                message = "Đã xoá món khỏi giỏ hàng.";
            } else if ("clear".equals(action)) {
                cart.clear();
                message = "Đã xoá toàn bộ giỏ hàng.";
            }
        } catch (Exception e) {
            error = "Không thể cập nhật giỏ hàng. Vui lòng thử lại.";
        }

        session.setAttribute("cart", cart);
        if (message != null) {
            session.setAttribute("cartMessage", message);
        }
        if (error != null) {
            session.setAttribute("cartError", error);
        }

        response.sendRedirect(getRedirectUrl(request));
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

    private void addToCart(List<CartItem> cart, Product product, int quantity) {
        for (CartItem item : cart) {
            if (item.getProduct() != null && item.getProduct().getProductId() == product.getProductId()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        cart.add(new CartItem(product, quantity));
    }

    private void changeQuantity(List<CartItem> cart, int productId, int amount) {
        Iterator<CartItem> iterator = cart.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getProduct() != null && item.getProduct().getProductId() == productId) {
                int newQuantity = item.getQuantity() + amount;
                if (newQuantity <= 0) {
                    iterator.remove();
                } else {
                    item.setQuantity(newQuantity);
                }
                return;
            }
        }
    }

    private void removeFromCart(List<CartItem> cart, int productId) {
        cart.removeIf(item -> item.getProduct() != null && item.getProduct().getProductId() == productId);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getRedirectUrl(HttpServletRequest request) {
        String redirect = request.getParameter("redirect");
        if (redirect == null || redirect.trim().isEmpty() || redirect.contains("://")) {
            return request.getContextPath() + "/menu";
        }
        if (redirect.startsWith(request.getContextPath())) {
            return redirect;
        }
        if (redirect.startsWith("/")) {
            return request.getContextPath() + redirect;
        }
        return request.getContextPath() + "/" + redirect;
    }
}
