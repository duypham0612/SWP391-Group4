package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.service.admin.UserService;
import com.cafe.service.manager.ShiftService;
import com.cafe.web.support.CsrfUtil;
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

/** Màn phân ca trực tiếp: tạo, sửa và gỡ ShiftAssignment. */
@WebServlet("/manager/shift")
public class ShiftServlet extends HttpServlet {

    private final ShiftService shiftService;
    private final UserService userService;

    public ShiftServlet() {
        this(new ShiftService(), new UserService());
    }

    ShiftServlet(ShiftService shiftService, UserService userService) {
        this.shiftService = java.util.Objects.requireNonNull(shiftService);
        this.userService = java.util.Objects.requireNonNull(userService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        LocalDate weekStart = parseWeekStart(req.getParameter("week"));
        try {
            req.setAttribute("weekStart", weekStart);
            req.setAttribute("weekDays", buildWeekDays(weekStart));
            req.setAttribute("prevWeek", weekStart.minusWeeks(1));
            req.setAttribute("nextWeek", weekStart.plusWeeks(1));
            req.setAttribute("assignments",
                    shiftService.getWeekSchedule(branchId, weekStart));
            req.setAttribute("staff", userService.getUserListByBranch(branchId));
            req.setAttribute("pageTitle", "Lịch làm việc");
            req.getRequestDispatcher(
                    "/WEB-INF/views/manager/shift-calendar.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF");
            return;
        }
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        String action = req.getParameter("action");
        String week = req.getParameter("week");
        String redirect = req.getContextPath() + "/manager/shift" +
                (week != null && !week.isBlank() ? "?week=" + week : "");
        try {
            if ("assign".equals(action)) {
                shiftService.assignShift(
                        trim(req.getParameter("shiftName")),
                        LocalTime.parse(req.getParameter("startTime")),
                        LocalTime.parse(req.getParameter("endTime")),
                        Integer.parseInt(req.getParameter("userId")),
                        LocalDate.parse(req.getParameter("workDate")),
                        branchId);
                req.getSession().setAttribute("flashOk", "Đã xếp ca cho nhân viên.");
            } else if ("update".equals(action)) {
                shiftService.updateShift(
                        Integer.parseInt(req.getParameter("assignmentId")),
                        trim(req.getParameter("shiftName")),
                        LocalTime.parse(req.getParameter("startTime")),
                        LocalTime.parse(req.getParameter("endTime")),
                        Integer.parseInt(req.getParameter("userId")),
                        LocalDate.parse(req.getParameter("workDate")),
                        branchId);
                req.getSession().setAttribute("flashOk", "Đã cập nhật ca làm.");
            } else if ("unassign".equals(action)) {
                shiftService.unassignShift(
                        Integer.parseInt(req.getParameter("assignmentId")), branchId);
                req.getSession().setAttribute("flashOk", "Đã gỡ ca làm.");
            } else {
                throw new BusinessException("Thao tác phân ca không hợp lệ.");
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (DateTimeParseException e) {
            req.getSession().setAttribute(
                    "flashError", "Định dạng ngày/giờ không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute(
                    "flashError", "Dữ liệu ca làm không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private LocalDate parseWeekStart(String week) {
        LocalDate base;
        try {
            base = week == null || week.isBlank()
                    ? LocalDate.now() : LocalDate.parse(week);
        } catch (DateTimeParseException e) {
            base = LocalDate.now();
        }
        return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate[] buildWeekDays(LocalDate weekStart) {
        LocalDate[] days = new LocalDate[7];
        for (int i = 0; i < days.length; i++) {
            days[i] = weekStart.plusDays(i);
        }
        return days;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
