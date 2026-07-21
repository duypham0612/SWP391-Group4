package com.cafe.controller.manager;

import com.cafe.service.shared.BaristaAuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Manager read-only audit of barista operational actions. */
@WebServlet("/manager/barista-history")
public class BaristaHistoryServlet extends HttpServlet {
    private final BaristaAuditService service = new BaristaAuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("history", service.all(branchId));
            req.setAttribute("pageTitle", "Lịch sử thao tác pha chế");
            req.getRequestDispatcher("/WEB-INF/views/manager/barista-history.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
