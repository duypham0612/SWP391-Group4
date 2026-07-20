package com.cafe.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Legacy modifier admin screen is retired; size is configured on Product. */
@WebServlet("/admin/modifier")
public class ModifierServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        redirectToProduct(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        redirectToProduct(req, resp);
    }

    private void redirectToProduct(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.getSession().setAttribute("flashOk",
                "Màn tuỳ chọn modifier đã được thay bằng cấu hình size trong Sản phẩm. Đá và đường là tuỳ chọn cố định.");
        resp.sendRedirect(req.getContextPath() + "/admin/product");
    }
}
