package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.cashier.TableSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** C3 · TableServlet → /cashier/table. Sơ đồ bàn + phiên bàn. */
@WebServlet("/cashier/table")
public class TableServlet extends HttpServlet {

    private final TableSessionService service = new TableSessionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            boolean showHidden = "1".equals(req.getParameter("showHidden"));
            req.setAttribute("showHidden", showHidden);
            req.setAttribute("tables", service.getFloorMap(branchId, showHidden));
            req.setAttribute("pageTitle", "Sơ đồ bàn");
            req.getRequestDispatcher("/WEB-INF/views/cashier/table-map.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        String ctx = req.getContextPath();
        try {
            if ("openTable".equals(action)) {
                int tableId = Integer.parseInt(req.getParameter("tableId"));
                int sessionId = service.openSession(branchId, tableId, userId);
                resp.sendRedirect(ctx + "/cashier/pos?sessionId=" + sessionId);
                return;
            } else if ("closeTable".equals(action)) {
                service.closeSession(Integer.parseInt(req.getParameter("sessionId")));
            } else if ("setStatus".equals(action)) {
                service.setTableStatus(Integer.parseInt(req.getParameter("tableId")), req.getParameter("status"));
            } else if ("saveTable".equals(action)) {
                String id = req.getParameter("tableId");
                Integer tableId = id == null || id.isBlank() ? null : Integer.parseInt(id);
                int capacity = Integer.parseInt(req.getParameter("capacity"));
                service.saveTable(tableId, branchId, req.getParameter("tableNumber"), capacity);
                req.getSession().setAttribute("flashOk", tableId == null ? "Đã thêm bàn mới." : "Đã cập nhật bàn.");
            } else if ("setVisibility".equals(action)) {
                service.setTableVisibility(Integer.parseInt(req.getParameter("tableId")), branchId,
                        Boolean.parseBoolean(req.getParameter("visible")));
                req.getSession().setAttribute("flashOk", "Đã cập nhật hiển thị bàn.");
            } else if ("mergeTables".equals(action)) {
                service.mergeTables(branchId, Integer.parseInt(req.getParameter("sourceTableId")),
                        Integer.parseInt(req.getParameter("destinationTableId")), userId);
                req.getSession().setAttribute("flashOk", "Đã ghép bàn và gom đơn vào bàn đích.");
            } else if ("unmergeTable".equals(action)) {
                service.unmergeTable(branchId, Integer.parseInt(req.getParameter("tableId")));
                req.getSession().setAttribute("flashOk", "Đã tách bàn khỏi nhóm ghép.");
            }
            resp.sendRedirect(ctx + "/cashier/table?showHidden=1");
        } catch (IllegalArgumentException | IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/cashier/table?showHidden=1");
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                req.getSession().setAttribute("flashError", "Tên bàn đã tồn tại trong chi nhánh.");
                resp.sendRedirect(ctx + "/cashier/table?showHidden=1");
            } else {
                throw new ServletException(e);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }
}
