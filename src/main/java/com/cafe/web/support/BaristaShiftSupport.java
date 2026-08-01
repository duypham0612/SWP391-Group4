package com.cafe.web.support;

import com.cafe.common.BusinessDay;
import com.cafe.model.ShiftClockStatus;
import com.cafe.model.User;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Web policy dùng chung cho các màn Barista; không giữ state theo request. */
public final class BaristaShiftSupport {
    public static final String OFF_SHIFT_MESSAGE =
            "Bạn đang ngoài ca — cần vào ca trước khi thao tác.";
    public static final String MY_QUEUE_PATH = "/barista/kds?owner=mine";

    private final AttendanceService attendanceService;
    private final OrderService orderService;

    public BaristaShiftSupport() {
        this(new AttendanceService(), new OrderService());
    }

    public BaristaShiftSupport(AttendanceService attendanceService, OrderService orderService) {
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.orderService = Objects.requireNonNull(orderService);
    }

    public void expose(HttpServletRequest request, String selfPath) throws Exception {
        ShiftClockStatus status = status(request);
        request.setAttribute("clockStatus", status);
        request.setAttribute("onShift", status != null && status.isCanClockOut());
        request.setAttribute("clockPostUrl", request.getContextPath() + selfPath);
    }

    public boolean onShift(HttpServletRequest request) {
        try {
            ShiftClockStatus status = status(request);
            return status != null && status.isCanClockOut();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean guardWrite(HttpServletRequest request, HttpServletResponse response, String selfPath)
            throws IOException {
        if (onShift(request)) return false;
        request.getSession().setAttribute("flashError", OFF_SHIFT_MESSAGE);
        response.sendRedirect(request.getContextPath() + selfPath);
        return true;
    }

    public boolean blockedOffShift(HttpServletRequest request) {
        if (onShift(request)) return false;
        request.getSession().setAttribute("flashError", OFF_SHIFT_MESSAGE);
        return true;
    }

    public String handleClock(HttpServletRequest request, String action, String selfPath) {
        if (!"clockIn".equals(action) && !"clockOut".equals(action)) return null;
        User user = SessionUtil.currentUser(request);
        int userId = user == null ? 0 : user.getUserId();
        int branchId = BranchContext.requireBranchId(request);
        try {
            if ("clockIn".equals(action)) {
                attendanceService.clockIn(userId, branchId);
                request.getSession().setAttribute("flashOk", "Đã vào ca.");
            } else if (hasPendingBrew(request, branchId, userId)) {
                return MY_QUEUE_PATH;
            } else {
                attendanceService.clockOut(userId, branchId);
                request.getSession().setAttribute("flashOk", "Đã tan ca.");
            }
        } catch (IllegalStateException e) {
            request.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            request.getSession().setAttribute("flashError", "Không chấm công được: " + e.getMessage());
        }
        return selfPath;
    }

    private boolean hasPendingBrew(HttpServletRequest request, int branchId, int userId) throws Exception {
        int pending = orderService.countMyMakingItems(branchId, userId);
        if (pending <= 0) return false;
        request.getSession().setAttribute("flashError", "Bạn còn " + pending
                + " ly đang pha — bấm “Xong” hoặc “Trả lại chờ” cho từng ly rồi mới tan ca được.");
        return true;
    }

    private ShiftClockStatus status(HttpServletRequest request) throws Exception {
        User user = SessionUtil.currentUser(request);
        if (user == null) return null;
        int branchId = BranchContext.requireBranchId(request);
        return attendanceService.getMyShiftStatus(user.getUserId(), branchId, BusinessDay.todayVn());
    }
}
