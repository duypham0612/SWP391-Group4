package com.cafe.controller.admin;

import com.cafe.common.Constants;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.admin.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Landing sau đăng nhập: điều hướng tới dashboard theo role. */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = SessionUtil.currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }
        // Manager có dashboard riêng (M1) cần truy vấn số liệu → điều hướng tới servlet chuyên trách.
        if (Constants.ROLE_MANAGER.equals(u.getRoleCode())) {
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }
        String view;
        switch (u.getRoleCode() == null ? "" : u.getRoleCode()) {
            case Constants.ROLE_ADMIN:   view = "/WEB-INF/views/admin/dashboard.jsp";   break;
            case Constants.ROLE_MANAGER: view = "/WEB-INF/views/manager/dashboard.jsp"; break;
            case Constants.ROLE_CASHIER: view = "/WEB-INF/views/cashier/dashboard.jsp"; break;
            case Constants.ROLE_BARISTA: view = "/WEB-INF/views/barista/dashboard.jsp"; break;
            default:
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Role không hợp lệ");
                return;
        }
        if (Constants.ROLE_ADMIN.equals(u.getRoleCode())) {
            try { req.setAttribute("summary", reportService.getChainSummary()); }
            catch (Exception e) { throw new ServletException(e); }
        }
        req.setAttribute("pageTitle", "Bảng điều khiển");
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
