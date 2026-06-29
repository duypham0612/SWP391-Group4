package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.User;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.RoleService;
import com.cafe.service.admin.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** A1 · UserServlet → /admin/user. Actions: list/create/update/toggleStatus/resetPassword/assignBranch. */
@WebServlet("/admin/user")
public class UserServlet extends HttpServlet {

    private final UserService service = new UserService();
    private final RoleService roleService = new RoleService();
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                User u = new User();
                u.setStatus("ACTIVE");
                req.setAttribute("staff", u);
                forwardForm(req, resp, "Thêm nhân sự");
            } else if ("edit".equals(action)) {
                User u = service.getUser(Integer.parseInt(req.getParameter("id")));
                if (u == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("staff", u);
                forwardForm(req, resp, "Sửa nhân sự");
            } else {
                Integer roleId = parseFilter(req.getParameter("roleId"));
                Integer branchId = parseFilter(req.getParameter("branchId"));
                req.setAttribute("staffList", service.getUserList(roleId, branchId));
                req.setAttribute("roles", roleService.getRoleList());
                req.setAttribute("branches", branchService.getBranchList());
                req.setAttribute("fRoleId", roleId);
                req.setAttribute("fBranchId", branchId);
                req.setAttribute("pageTitle", "Nhân sự");
                req.getRequestDispatcher("/WEB-INF/views/admin/user-list.jsp").forward(req, resp);
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
            if ("toggleStatus".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                String to = "LOCKED".equals(req.getParameter("current")) ? "ACTIVE" : "LOCKED";
                service.setUserStatus(id, to);
                resp.sendRedirect(ctx + "/admin/user");
                return;
            }
            User u = bind(req);
            String password = req.getParameter("password");
            boolean creating = u.getUserId() == 0;
            String error = validate(u, password, creating);
            if (error != null) {
                req.setAttribute("staff", u);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, creating ? "Thêm nhân sự" : "Sửa nhân sự");
                return;
            }
            if (creating) {
                service.createUser(u, password);
            } else {
                service.updateUser(u);
                if (password != null && !password.isBlank()) service.resetPassword(u.getUserId(), password);
            }
            resp.sendRedirect(ctx + "/admin/user");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private User bind(HttpServletRequest req) {
        User u = new User();
        String id = req.getParameter("userId");
        if (id != null && !id.isBlank()) u.setUserId(Integer.parseInt(id));
        u.setUsername(trim(req.getParameter("username")));
        u.setFullName(trim(req.getParameter("fullName")));
        u.setEmail(trim(req.getParameter("email")));
        u.setPhone(trim(req.getParameter("phone")));
        String role = req.getParameter("roleId");
        if (role != null && !role.isBlank()) u.setRoleId(Integer.parseInt(role));
        String branch = req.getParameter("branchId");
        u.setBranchId(branch == null || branch.isBlank() ? null : Integer.parseInt(branch));
        String status = req.getParameter("status");
        u.setStatus(status == null || status.isBlank() ? "ACTIVE" : status);
        return u;
    }

    private String validate(User u, String password, boolean creating) throws Exception {
        if (u.getUsername() == null || u.getUsername().isBlank()) return "Tên đăng nhập không được để trống.";
        if (u.getFullName() == null || u.getFullName().isBlank()) return "Họ tên không được để trống.";
        if (u.getRoleId() <= 0) return "Vui lòng chọn vai trò.";
        if (creating && (password == null || password.length() < 6)) return "Mật khẩu tối thiểu 6 ký tự.";
        if (!creating && password != null && !password.isBlank() && password.length() < 6)
            return "Mật khẩu mới tối thiểu 6 ký tự.";
        if (service.usernameTaken(u.getUsername(), u.getUserId())) return "Tên đăng nhập đã tồn tại.";
        if (!"ACTIVE".equals(u.getStatus()) && !"LOCKED".equals(u.getStatus())) return "Trạng thái không hợp lệ.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try {
            req.setAttribute("roles", roleService.getRoleList());
            req.setAttribute("branches", branchService.getBranchListActive());
        } catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    /** Param lọc → Integer; rỗng/"0"/không phải số = null (bỏ lọc). */
    private Integer parseFilter(String s) {
        if (s == null || s.isBlank()) return null;
        try { int v = Integer.parseInt(s.trim()); return v <= 0 ? null : v; }
        catch (NumberFormatException e) { return null; }
    }
}
