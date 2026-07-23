package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.model.ShiftTemplate;
import com.cafe.service.manager.ShiftService;
import com.cafe.service.admin.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/** M2 · ShiftServlet → /manager/shift. week | createTemplate | assign | unassign. */
@WebServlet("/manager/shift")
public class ShiftServlet extends HttpServlet {

    private final ShiftService shiftService = new ShiftService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        LocalDate weekStart = parseWeekStart(req.getParameter("week"));
        try {
            req.setAttribute("weekStart", weekStart);
            req.setAttribute("weekDays", buildWeekDays(weekStart));
            req.setAttribute("prevWeek", weekStart.minusWeeks(1));
            req.setAttribute("nextWeek", weekStart.plusWeeks(1));
            req.setAttribute("templates", shiftService.getShiftTemplates(branchId));
            req.setAttribute("assignments", shiftService.getWeekSchedule(branchId, weekStart));
            req.setAttribute("staff", userService.getUserListByBranch(branchId));
            req.setAttribute("pageTitle", "Lịch làm việc");
            req.getRequestDispatcher("/WEB-INF/views/manager/shift-calendar.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        String action = req.getParameter("action");
        String week = req.getParameter("week");
        String redirect = req.getContextPath() + "/manager/shift" + (week != null && !week.isBlank() ? "?week=" + week : "");
        try {
            if ("createTemplate".equals(action)) {
                ShiftTemplate t = new ShiftTemplate();
                t.setBranchId(branchId);
                t.setName(trim(req.getParameter("name")));
                t.setStartTime(LocalTime.parse(req.getParameter("startTime")));
                t.setEndTime(LocalTime.parse(req.getParameter("endTime")));
                if (t.getName() == null || t.getName().isBlank() || !t.getStartTime().isBefore(t.getEndTime())) {
                    req.getSession().setAttribute("flashError", "Tên ca và giờ bắt đầu < giờ kết thúc là bắt buộc.");
                } else {
                    shiftService.createShiftTemplate(t);
                }
            } else if ("deleteTemplate".equals(action)) {
                shiftService.deleteShiftTemplate(Integer.parseInt(req.getParameter("templateId")));
            } else if ("assign".equals(action)) {
                int templateId = Integer.parseInt(req.getParameter("templateId"));
                int userId = Integer.parseInt(req.getParameter("userId"));
                LocalDate date = LocalDate.parse(req.getParameter("workDate"));
                shiftService.assignShift(templateId, userId, date);
            } else if ("unassign".equals(action)) {
                shiftService.unassignShift(Integer.parseInt(req.getParameter("assignmentId")));
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (DateTimeParseException e) {
            req.getSession().setAttribute("flashError", "Định dạng ngày/giờ không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Dữ liệu ca làm không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Đầu tuần (Thứ 2) chứa ngày week, hoặc tuần hiện tại. */
    private LocalDate parseWeekStart(String week) {
        LocalDate base;
        try { base = (week == null || week.isBlank()) ? LocalDate.now() : LocalDate.parse(week); }
        catch (DateTimeParseException e) { base = LocalDate.now(); }
        return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate[] buildWeekDays(LocalDate weekStart) {
        LocalDate[] days = new LocalDate[7];
        for (int i = 0; i < 7; i++) days[i] = weekStart.plusDays(i);
        return days;
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
