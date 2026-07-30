package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.shared.InventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Manager xem và đính chính mẻ pha sai của chính chi nhánh. */
@WebServlet("/manager/prep")
public class ManagerPrepServlet extends HttpServlet {
    private final InventoryService inventoryService = new InventoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("batches", inventoryService.getRecentPrepBatches(branchId, 50));
            req.setAttribute("pendingBatches", inventoryService.getPendingApprovalBatches(branchId));
            req.setAttribute("unreviewedBatches", inventoryService.getUnreviewedBatches(branchId));
            req.setAttribute("pageTitle", "Quản lý mẻ pha sẵn");
            req.getRequestDispatcher("/WEB-INF/views/manager/prep-list.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User user = SessionUtil.currentUser(req);
        try {
            String action = req.getParameter("action");
            int batchId = Integer.parseInt(req.getParameter("prepBatchId"));
            if ("cancelBatch".equals(action)) {
                boolean cancelled = inventoryService.cancelPrepBatchByManager(
                        branchId, batchId, user.getUserId());
                req.getSession().setAttribute("flashOk", cancelled
                        ? "Đã hủy mẻ sai và hoàn tồn qua sổ cái."
                        : "Mẻ đã được hủy trước đó; tồn kho không thay đổi.");
            } else if ("approveBatch".equals(action)) {
                inventoryService.approvePrepBatch(branchId, batchId, user.getUserId());
                req.getSession().setAttribute("flashOk", "Đã duyệt mẻ — nguyên liệu pha sẵn đã tính vào tồn bán được.");
            } else if ("rejectBatch".equals(action)) {
                inventoryService.rejectPrepBatch(branchId, batchId, user.getUserId());
                req.getSession().setAttribute("flashOk", "Đã từ chối mẻ — nguyên liệu thô đã được hoàn lại tồn kho.");
            } else if ("markReviewed".equals(action)) {
                inventoryService.markPrepBatchReviewed(branchId, batchId, user.getUserId());
                req.getSession().setAttribute("flashOk", "Đã đánh dấu đã xem.");
            } else {
                throw new BusinessException("Thao tác không hợp lệ.");
            }
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã mẻ không hợp lệ.");
        } catch (Exception e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/manager/prep");
    }
}
