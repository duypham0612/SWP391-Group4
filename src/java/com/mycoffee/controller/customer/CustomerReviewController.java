package com.mycoffee.controller.customer;

import com.mycoffee.dao.CustomerDAO;
import com.mycoffee.dao.FeedbackDAO;
import com.mycoffee.dao.OrderDAO;
import com.mycoffee.model.Customer;
import com.mycoffee.model.Order;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CustomerReviewController", urlPatterns = {"/customer-review"})
public class CustomerReviewController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        if (user != null && user.getRoleId() == User.ROLE_CUSTOMER) {
            Customer customer = customerDAO.getCustomerById(user.getUserId());
            request.setAttribute("customerInfo", customer);

            Order order = resolveReviewOrder(request, user.getUserId());
            if (order != null) {
                request.setAttribute("order", order);
                request.setAttribute("orderDetails", orderDAO.getOrderDetails(order.getOrderId()));
            }
        }

        request.getRequestDispatcher("customer_review.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

        int orderId = parseInt(request.getParameter("orderId"), 0);
        if (orderId <= 0) {
            session.setAttribute("reviewError", "Không tìm thấy đơn hàng cần đánh giá.");
            response.sendRedirect(request.getContextPath() + "/customer-review");
            return;
        }

        List<Integer> ratings = collectRatings(request);
        if (ratings.isEmpty()) {
            session.setAttribute("reviewError", "Vui lòng chọn ít nhất một số sao trước khi gửi đánh giá.");
            response.sendRedirect(request.getContextPath() + "/customer-review?orderId=" + orderId);
            return;
        }

        int averageRating = Math.max(1, Math.min(5, Math.round(sum(ratings) / (float) ratings.size())));
        String comment = buildComment(request);
        boolean success = feedbackDAO.addFeedback(orderId, averageRating, comment);

        if (success) {
            session.setAttribute("reviewMessage", "Cảm ơn bạn! Đánh giá đã được ghi nhận.");
        } else {
            session.setAttribute("reviewError", "Chưa thể lưu đánh giá. Vui lòng thử lại sau.");
        }
        response.sendRedirect(request.getContextPath() + "/customer-review?orderId=" + orderId);
    }

    private Order resolveReviewOrder(HttpServletRequest request, int customerId) {
        int orderId = parseInt(request.getParameter("orderId"), 0);
        List<Order> orders = orderDAO.getOrdersByCustomerId(customerId);
        if (orders.isEmpty()) {
            return null;
        }

        if (orderId > 0) {
            for (Order order : orders) {
                if (order.getOrderId() == orderId) {
                    return order;
                }
            }
        }

        for (Order order : orders) {
            if ("Completed".equalsIgnoreCase(order.getOrderStatus())) {
                return order;
            }
        }
        return orders.get(0);
    }

    private List<Integer> collectRatings(HttpServletRequest request) {
        List<Integer> ratings = new ArrayList<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("itemRating_")) {
                int rating = parseInt(request.getParameter(name), 0);
                if (rating >= 1 && rating <= 5) {
                    ratings.add(rating);
                }
            }
        }
        int serviceRating = parseInt(request.getParameter("serviceRating"), 0);
        if (serviceRating >= 1 && serviceRating <= 5) {
            ratings.add(serviceRating);
        }
        return ratings;
    }

    private String buildComment(HttpServletRequest request) {
        StringBuilder comment = new StringBuilder();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith("itemComment_")) {
                String value = request.getParameter(name);
                if (value != null && !value.trim().isEmpty()) {
                    if (comment.length() > 0) comment.append("\n");
                    comment.append(value.trim());
                }
            }
        }

        String serviceComment = request.getParameter("serviceComment");
        if (serviceComment != null && !serviceComment.trim().isEmpty()) {
            if (comment.length() > 0) comment.append("\n");
            comment.append("Dịch vụ: ").append(serviceComment.trim());
        }

        String tags = request.getParameter("serviceTags");
        if (tags != null && !tags.trim().isEmpty()) {
            if (comment.length() > 0) comment.append("\n");
            comment.append("Điểm nổi bật: ").append(tags.trim());
        }
        return comment.toString();
    }

    private int sum(List<Integer> values) {
        int total = 0;
        for (Integer value : values) {
            total += value;
        }
        return total;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
