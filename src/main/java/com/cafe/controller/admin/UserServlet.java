package com.cafe.controller.admin;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.web.support.CsrfUtil;
import com.cafe.model.Branch;
import com.cafe.model.Role;
import com.cafe.model.User;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.RoleService;
import com.cafe.service.admin.UserService;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.UserForm;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Admin staff accounts. */
@WebServlet("/admin/user")
public class UserServlet extends HttpServlet {

    private final UserService service;
    private final RoleService roleService;
    private final BranchService branchService;

    public UserServlet() {
        this(new UserService(), new RoleService(), new BranchService());
    }

    UserServlet(UserService service, RoleService roleService, BranchService branchService) {
        this.service = Objects.requireNonNull(service, "service");
        this.roleService = Objects.requireNonNull(roleService, "roleService");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                User u = new User();
                u.setStatus("ACTIVE");
                int assignmentBranchId = parsePositiveInt(req.getParameter("branchId"));
                if (assignmentBranchId > 0) {
                    Branch assignmentBranch = branchService.getBranch(assignmentBranchId);
                    if (assignmentBranch == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    if (assignmentBranch.getManagerUserId() != null) {
                        req.getSession().setAttribute("flashError", "Chi nhánh đã có quản lý phụ trách.");
                        resp.sendRedirect(req.getContextPath() + "/admin/branch");
                        return;
                    }
                    Role managerRole = roleByCode(Constants.ROLE_MANAGER);
                    u.setRoleId(managerRole.getRoleId());
                    u.setBranchId(assignmentBranchId);
                    u.setBranchName(assignmentBranch.getName());
                    setAssignmentAttributes(req, assignmentBranch, managerRole);
                }
                req.setAttribute("staff", u);
                forwardForm(req, resp, "Thêm nhân sự");
            } else if ("edit".equals(action)) {
                User u = service.getUser(Integer.parseInt(req.getParameter("id")));
                if (u == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if (Constants.ROLE_ADMIN.equals(u.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tài khoản Admin hệ thống không thể chỉnh sửa.");
                    resp.sendRedirect(req.getContextPath() + "/admin/user");
                    return;
                }
                req.setAttribute("staff", u);
                forwardForm(req, resp, "Sửa nhân sự");
            } else {
                Integer roleId = parseFilter(req.getParameter("roleId"));
                Integer branchId = parseFilter(req.getParameter("branchId"));
                String q = trim(req.getParameter("q"));
                int page = parsePage(req.getParameter("page"));
                int pageSize = 6;
                int total = service.countUsers(roleId, branchId, q);
                int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                if (page > totalPages) page = totalPages;
                int offset = (page - 1) * pageSize;

                req.setAttribute("staffList", service.getUserList(roleId, branchId, q, offset, pageSize));
                req.setAttribute("roles", roleService.getRoleList());
                req.setAttribute("branches", branchService.getBranchList());
                req.setAttribute("fRoleId", roleId);
                req.setAttribute("fBranchId", branchId);
                req.setAttribute("q", q);
                req.setAttribute("page", page);
                req.setAttribute("rowStart", offset);
                req.setAttribute("totalPages", totalPages);
                req.setAttribute("total", total);
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
                service.toggleUserStatus(id);
                req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái nhân sự.");
                resp.sendRedirect(ctx + "/admin/user");
                return;
            }
            UserForm form = UserForm.from(req);
            User u = form.user();
            String password = form.password();
            boolean creating = u.getUserId() == 0;
            int assignmentBranchId = form.assignmentBranchId();
            boolean assignmentMode = creating && assignmentBranchId > 0;
            if (assignmentMode) {
                Branch assignmentBranch = branchService.getBranch(assignmentBranchId);
                if (assignmentBranch == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                Role managerRole = roleByCode(Constants.ROLE_MANAGER);
                u.setRoleId(managerRole.getRoleId());
                u.setBranchId(assignmentBranchId);
                u.setBranchName(assignmentBranch.getName());
                setAssignmentAttributes(req, assignmentBranch, managerRole);
            }

            if (!creating) {
                User existing = service.getUser(u.getUserId());
                if (existing == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if (Constants.ROLE_ADMIN.equals(existing.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tài khoản Admin hệ thống không thể chỉnh sửa.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
            }
            if (creating) {
                try {
                    service.createUser(u, password);
                    req.getSession().setAttribute("flashOk", assignmentMode
                            ? "Đã phân công quản lý chi nhánh thành công."
                            : "Đã thêm nhân sự thành công.");
                } catch (BusinessException e) {
                    showSaveError(req, resp, u, e.getMessage(), true);
                    return;
                }
            } else {
                try {
                    service.updateUser(u);
                    req.getSession().setAttribute("flashOk", "Đã cập nhật nhân sự thành công.");
                } catch (BusinessException e) {
                    showSaveError(req, resp, u, e.getMessage(), false);
                    return;
                }
            }
            resp.sendRedirect(ctx + (assignmentMode ? "/admin/branch" : "/admin/user"));
        } catch (BusinessException | FormBindingException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/admin/user");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try {
            List<Role> roles = roleService.getRoleList();
            roles.removeIf(r -> Constants.ROLE_ADMIN.equals(r.getCode()));
            req.setAttribute("roles", roles);
            req.setAttribute("branches", branchService.getBranchListActive());
        } catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req, resp);
    }

    private Role roleByCode(String code) throws Exception {
        for (Role r : roleService.getRoleList()) {
            if (code.equals(r.getCode())) return r;
        }
        throw new IllegalStateException("Không tìm thấy vai trò " + code + ".");
    }

    private void setAssignmentAttributes(HttpServletRequest req, Branch branch, Role role) {
        req.setAttribute("assignmentMode", true);
        req.setAttribute("assignmentBranch", branch);
        req.setAttribute("assignmentRole", role);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private void showSaveError(HttpServletRequest req, HttpServletResponse resp, User user,
                               String message, boolean creating)
            throws ServletException, IOException {
        req.setAttribute("staff", user);
        req.setAttribute("errorMsg", message);
        forwardForm(req, resp, creating ? "Thêm nhân sự" : "Sửa nhân sự");
    }

    private Integer parseFilter(String s) {
        if (s == null || s.isBlank()) return null;
        try { int v = Integer.parseInt(s.trim()); return v <= 0 ? null : v; }
        catch (NumberFormatException e) { return null; }
    }

    private int parsePage(String s) {
        if (s == null || s.isBlank()) return 1;
        try { return Math.max(1, Integer.parseInt(s.trim())); }
        catch (NumberFormatException e) { return 1; }
    }

    private int parsePositiveInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            int value = Integer.parseInt(s.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
