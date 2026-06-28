package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.manager.AttendanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** M3 · AttendanceServlet → /manager/attendance. list | approve | reject | edit. */
@WebServlet("/manager/attendance")
public class AttendanceServlet extends HttpServlet {

    private final AttendanceService service = new AttendanceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        String status = req.getParameter("status");
        if (status == null || status.isBlank()) status = "PENDING";
        try {
            req.setAttribute("status", status);
            req.setAttribute("attendances", service.getAttendanceByStatus(branchId, status));
            req.setAttribute("pageTitle", "Chấm công");
            req.getRequestDispatcher("/WEB-INF/views/manager/attendance-list.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        int approverId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        String status = req.getParameter("status");
        String redirect = req.getContextPath() + "/manager/attendance"
                + (status != null && !status.isBlank() ? "?status=" + status : "");
        try {
            int id = Integer.parseInt(req.getParameter("attendanceId"));
            if ("approve".equals(action)) {
                service.approveAttendance(id, approverId);
            } else if ("reject".equals(action)) {
                service.rejectAttendance(id, approverId);
            } else if ("edit".equals(action)) {
                LocalDateTime ci = parse(req.getParameter("checkInAt"));
                LocalDateTime co = parse(req.getParameter("checkOutAt"));
                service.updateAttendance(id, ci, co);
            }
            resp.sendRedirect(redirect);
        } catch (DateTimeParseException e) {
            req.getSession().setAttribute("flashError", "Định dạng thời gian không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** input datetime-local → LocalDateTime; rỗng = null. */
    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDateTime.parse(s);
    }
}
