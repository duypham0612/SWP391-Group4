package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Branch;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Admin branch management. */
@WebServlet("/admin/branch")
public class BranchServlet extends HttpServlet {

    private final BranchService service = new BranchService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("branch", new Branch());
                forwardForm(req, resp, "Thêm chi nhánh");
            } else if ("edit".equals(action)) {
                Branch b = service.getBranch(Integer.parseInt(req.getParameter("id")));
                if (b == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("branch", b);
                forwardForm(req, resp, "Sửa chi nhánh");
            } else {
                req.setAttribute("branches", service.getBranchList());
                req.setAttribute("pageTitle", "Chi nhánh");
                req.getRequestDispatcher("/WEB-INF/views/admin/branch-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                service.toggleActive(Integer.parseInt(req.getParameter("id")));
                req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái chi nhánh.");
                resp.sendRedirect(ctx + "/admin/branch");
                return;
            }
            Branch b = bind(req);
            String error = validate(b);
            if (error != null) {
                if (b.getBranchId() != 0) {
                    Branch existing = service.getBranch(b.getBranchId());
                    if (existing != null) b.setCode(existing.getCode());
                }
                req.setAttribute("branch", b);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, b.getBranchId() == 0 ? "Thêm chi nhánh" : "Sửa chi nhánh");
                return;
            }
            if (b.getBranchId() == 0) {
                service.createBranch(b);
                req.getSession().setAttribute("flashOk", "Đã thêm chi nhánh thành công.");
            } else {
                service.updateBranch(b);
                req.getSession().setAttribute("flashOk", "Đã cập nhật chi nhánh thành công.");
            }
            resp.sendRedirect(ctx + "/admin/branch");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Branch bind(HttpServletRequest req) {
        Branch b = new Branch();
        String id = req.getParameter("branchId");
        if (id != null && !id.isBlank()) b.setBranchId(Integer.parseInt(id));
        b.setName(trim(req.getParameter("name")));
        b.setAddress(trim(req.getParameter("address")));
        b.setPhone(null);
        b.setActive(req.getParameter("active") != null);
        b.setOpenTime(parseTime(req.getParameter("openTime")));
        b.setCloseTime(parseTime(req.getParameter("closeTime")));
        int managerUserId = parsePositiveInt(req.getParameter("managerUserId"));
        b.setManagerUserId(managerUserId <= 0 ? null : managerUserId);
        return b;
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalTime.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    private String validate(Branch b) throws Exception {
        if (b.getName() == null || b.getName().isBlank()) return "Tên chi nhánh không được để trống.";
        if (b.getAddress() == null || b.getAddress().isBlank()) return "Địa chỉ không được để trống.";
        if ((b.getOpenTime() == null) != (b.getCloseTime() == null))
            return "Giờ mở cửa và giờ đóng cửa phải nhập cả hai hoặc để trống cả hai.";
        if (b.getOpenTime() != null && !b.getOpenTime().isBefore(b.getCloseTime()))
            return "Giờ mở cửa phải trước giờ đóng cửa trong cùng ngày.";
        if (b.getManagerUserId() != null && userService.getManagers().stream().noneMatch(u -> u.getUserId() == b.getManagerUserId()))
            return "Quản lý phụ trách không hợp lệ.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try { req.setAttribute("managers", userService.getManagers()); }
        catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/branch-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private int parsePositiveInt(String raw) {
        try {
            if (raw == null || raw.isBlank()) return 0;
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
