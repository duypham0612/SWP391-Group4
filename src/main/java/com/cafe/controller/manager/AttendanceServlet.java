package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** M3 · AttendanceServlet → /manager/attendance. 1 màn gộp: approveMany (tickbox) | reject | reopen | edit. */
@WebServlet("/manager/attendance")
public class AttendanceServlet extends HttpServlet {

    private final AttendanceService service;

    public AttendanceServlet() { this(new AttendanceService()); }
    AttendanceServlet(AttendanceService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        try {
            req.setAttribute("attendances", service.getBranchAttendance(branchId));
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
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/manager/attendance";
        try {
            if ("approveMany".equals(action)) {
                List<Integer> shown = parseIds(req.getParameterValues("shown"));
                Set<Integer> checked = new HashSet<>(parseIds(req.getParameterValues("approve")));
                service.setApprovalStates(shown, checked, approverId, branchId);
                req.getSession().setAttribute("flashOk", "Đã lưu chấm công (tick = duyệt).");
            } else if ("reject".equals(action)) {
                service.rejectAttendance(
                        Integer.parseInt(req.getParameter("assignmentId")), approverId, branchId);
            } else if ("reopen".equals(action)) {
                service.reopenAttendance(Integer.parseInt(req.getParameter("assignmentId")), branchId);
            } else if ("edit".equals(action)) {
                int id = Integer.parseInt(req.getParameter("assignmentId"));
                LocalDateTime ci = parse(req.getParameter("checkInAt"));
                LocalDateTime co = parse(req.getParameter("checkOutAt"));
                service.updateAttendance(id, branchId, ci, co);
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (DateTimeParseException e) {
            req.getSession().setAttribute("flashError", "Định dạng thời gian không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã chấm công không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private List<Integer> parseIds(String[] vals) {
        List<Integer> out = new ArrayList<>();
        if (vals != null) for (String v : vals) {
            try { out.add(Integer.parseInt(v)); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    /** input datetime-local → LocalDateTime; rỗng = null. */
    private LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDateTime.parse(s);
    }
}
