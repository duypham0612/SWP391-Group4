package com.cafe.controller.customer;

import com.cafe.service.shared.CatalogReadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Trang Home công khai → /home. Khách xem thực đơn theo danh mục (ảnh + giá),
 * không cần đăng nhập. Chỉ đọc catalog (không gắn bàn/đặt món).
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private final CatalogReadService catalog = new CatalogReadService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("sections", catalog.getPublicMenu());
            req.getRequestDispatcher("/WEB-INF/views/customer/home.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
