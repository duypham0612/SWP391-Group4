package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.service.admin.BranchService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Cài đặt vận hành của chính chi nhánh Manager phụ trách: giờ mở/đóng cửa (mốc ngày kinh doanh
 * ở Quầy pha chế) + ngưỡng cao điểm. Route /manager/* nên RbacFilter đã chặn đúng role;
 * branchId LẤY TỪ SESSION — Manager không sửa được chi nhánh khác (branch scoping).
 */
@WebServlet("/manager/branch-settings")
public class BranchSettingsServlet extends HttpServlet {

    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("branch", branchService.getBranch(branchId));
            req.setAttribute("pageTitle", "Cài đặt chi nhánh");
            req.getRequestDispatcher("/WEB-INF/views/manager/branch-settings.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        String self = req.getContextPath() + "/manager/branch-settings";
        try {
            String openRaw = req.getParameter("openTime");
            String closeRaw = req.getParameter("closeTime");
            String peakRaw = req.getParameter("peakThresholdCups");
            LocalTime open = parseTime(openRaw);
            LocalTime close = parseTime(closeRaw);
            Integer peak = parsePeak(peakRaw);
            if ((openRaw != null && !openRaw.isBlank() && open == null) || (closeRaw != null && !closeRaw.isBlank() && close == null) || peak == null) {
                req.getSession().setAttribute("flashError", "Giờ hoạt động hoặc ngưỡng cao điểm không đúng định dạng.");
                resp.sendRedirect(self); return;
            }
            String error = validate(open, close, peak);
            if (error != null) {
                req.getSession().setAttribute("flashError", error);
                resp.sendRedirect(self);
                return;
            }
            branchService.updateHoursAndPeak(branchId, open, close, peak);
            req.getSession().setAttribute("flashOk", "Đã lưu cài đặt chi nhánh.");
            resp.sendRedirect(self);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** "HH:mm" → LocalTime; rỗng = null (chưa khai giờ → mốc ngày cắt theo nửa đêm). */
    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalTime.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }

    /** Ngưỡng cao điểm; rỗng/không hợp lệ → 0 (dùng mặc định toàn hệ). */
    private Integer parsePeak(String s) {
        try { int value = s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim()); return value >= 0 ? value : null; }
        catch (NumberFormatException e) { return null; }
    }

    private String validate(LocalTime open, LocalTime close, int peak) {
        if ((open == null) != (close == null))
            return "Giờ mở/đóng phải nhập cả hai hoặc để trống cả hai.";
        // Cho phép đóng sớm hơn mở (bán qua nửa đêm); chỉ chặn khi trùng.
        if (open != null && close.equals(open))
            return "Giờ đóng và giờ mở cửa không được trùng nhau.";
        if (peak < 0) return "Ngưỡng cao điểm phải >= 0.";
        return null;
    }
}
