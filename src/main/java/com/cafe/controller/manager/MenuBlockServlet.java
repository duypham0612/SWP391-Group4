package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.shared.BranchMenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/manager/menu-block")
public class MenuBlockServlet extends HttpServlet {

    private final BranchMenuService service = new BranchMenuService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("openRequests", service.getOpenRequests(branchId));
            req.setAttribute("history", service.getRequestHistory(branchId, 20));
            req.setAttribute("pageTitle", "Món tạm hết");
            req.getRequestDispatcher("/WEB-INF/views/manager/menu-block.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int reviewerId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/manager/menu-block";
        try {
            int requestId = Integer.parseInt(req.getParameter("requestId"));
            String reviewNote = req.getParameter("reviewNote");
            if ("reject".equals(action)) {
                service.reopen86(branchId, requestId, reviewerId, reviewNote, true);
                req.getSession().setAttribute("flashOk", "Đã từ chối và mở bán lại món.");
            } else if ("reopen".equals(action)) {
                service.reopen86(branchId, requestId, reviewerId, reviewNote, false);
                req.getSession().setAttribute("flashOk", "Đã mở bán lại món.");
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã yêu cầu không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
