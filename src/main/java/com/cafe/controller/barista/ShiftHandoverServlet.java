package com.cafe.controller.barista;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Bàn giao ca: gửi đầu việc tới ca sau, nhận bàn giao và theo dõi việc tồn. */
@WebServlet("/barista/handover")
public class ShiftHandoverServlet extends HttpServlet {
    private static final String SELF = "/barista/handover";
    private final HandoverService service = new HandoverService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req);
        try {
            BaristaShift.expose(req, MyShiftServlet.PATH);
            int userId = user == null ? 0 : user.getUserId();
            req.setAttribute("handovers", service.getHandovers(branchId, userId));
            req.setAttribute("pendingHandoverCount", service.countUnacknowledgedForUser(branchId, userId));
            if (BaristaShift.onShift(req)) {
                try { req.setAttribute("receiverPreview", service.previewReceiver(branchId, userId)); }
                catch (IllegalStateException e) { req.setAttribute("receiverPreviewError", e.getMessage()); }
            }
            req.setAttribute("kpi", service.getKpi(branchId));
            req.setAttribute("expiredPrepBatchCount", service.countExpiredActivePrepBatches(branchId));
            req.setAttribute("pageTitle", "Bàn giao ca");
            req.getRequestDispatcher("/WEB-INF/views/barista/handover.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req); int userId = user == null ? 0 : user.getUserId();
        String action = req.getParameter("action");
        try {
            if ("create".equals(action) || "createAndClockOut".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn đang ngoài ca — cần vào ca trước khi lập bàn giao.");
                List<String> tasks = Arrays.asList(req.getParameterValues("task") == null ? new String[0] : req.getParameterValues("task"));
                if ("createAndClockOut".equals(action)) service.createHandoverAndClockOut(branchId, userId, req.getParameter("note"), tasks);
                else service.createHandover(branchId, userId, req.getParameter("note"), tasks);
                req.getSession().setAttribute("flashOk", "Đã gửi bàn giao" + ("createAndClockOut".equals(action) ? " và tan ca." : "."));
            } else if ("acknowledge".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn cần vào ca trước khi nhận bàn giao.");
                service.acknowledge(branchId, parsePositive(req, "handoverId"), userId);
                req.getSession().setAttribute("flashOk", "Đã xác nhận nhận bàn giao.");
            } else if ("updateTask".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn cần đang trong ca để cập nhật việc bàn giao.");
                service.updateTaskStatus(branchId, parsePositive(req, "handoverId"), parsePositive(req, "taskId"), req.getParameter("status"), userId);
                req.getSession().setAttribute("flashOk", "Đã cập nhật việc bàn giao.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) { req.getSession().setAttribute("flashError", e.getMessage()); }
        catch (Exception e) { throw new ServletException(e); }
        resp.sendRedirect(req.getContextPath() + SELF);
    }
    private static int parsePositive(HttpServletRequest req, String name) { try { int value = Integer.parseInt(req.getParameter(name)); if (value > 0) return value; } catch (NumberFormatException ignored) {} throw new IllegalArgumentException("Dữ liệu bàn giao không hợp lệ."); }
}
