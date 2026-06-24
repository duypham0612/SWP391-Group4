package com.mycoffee.controller.cashier;

import com.mycoffee.service.OrderService;
import java.io.BufferedReader;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "PayOSWebhookController", urlPatterns = {"/payment"})
public class PayOSWebhookController extends HttpServlet {
    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String payload = sb.toString();

        System.out.println(">>> NHẬN WEBHOOK TỪ PAYOS: " + payload);

        try {
            if (payload.contains("\"code\":\"00\"") && payload.contains("\"success\":true")) {
                int orderCodeStart = payload.indexOf("\"orderCode\":") + 12;
                int orderCodeEnd = payload.indexOf(",", orderCodeStart);
                String orderCodeStr = payload.substring(orderCodeStart, orderCodeEnd).trim();
                int orderId = Integer.parseInt(orderCodeStr);

                orderService.completeOrder(orderId);
                System.out.println(">>> ĐÃ CẬP NHẬT THÀNH CÔNG ĐƠN HÀNG #" + orderId);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"success\":true}");
        } catch (Exception e) {
            System.out.println("Lỗi xử lý Webhook: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}