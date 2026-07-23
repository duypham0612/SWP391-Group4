package com.cafe.controller.admin;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.model.Role;
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
import java.util.List;

/** Admin staff accounts. */
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
                forwardForm(req, resp, "Them nhan su");
            } else if ("edit".equals(action)) {
                User u = service.getUser(Integer.parseInt(req.getParameter("id")));
                if (u == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if (Constants.ROLE_ADMIN.equals(u.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tai khoan Admin he thong khong the chinh sua.");
                    resp.sendRedirect(req.getContextPath() + "/admin/user");
                    return;
                }
                req.setAttribute("staff", u);
                forwardForm(req, resp, "Sua nhan su");
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
                req.setAttribute("pageTitle", "Nhan su");
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
                User target = service.getUser(id);
                if (target != null && Constants.ROLE_ADMIN.equals(target.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tai khoan Admin luon hoat dong, khong the khoa.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
                String to = "LOCKED".equals(req.getParameter("current")) ? "ACTIVE" : "LOCKED";
                service.setUserStatus(id, to);
                resp.sendRedirect(ctx + "/admin/user");
                return;
            }
            User u = bind(req);
            String password = req.getParameter("password");
            boolean creating = u.getUserId() == 0;

            if (creating && u.getRoleId() == adminRoleId()) {
                req.setAttribute("staff", u);
                req.setAttribute("errorMsg", "He thong chi co 1 Admin toan chuoi, khong the tao them tai khoan Admin.");
                forwardForm(req, resp, "Them nhan su");
                return;
            }
            if (!creating) {
                User existing = service.getUser(u.getUserId());
                if (existing == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if (Constants.ROLE_ADMIN.equals(existing.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tai khoan Admin he thong khong the chinh sua.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
                applyLockedFields(u, existing);
            }

            String error = validate(u, password, creating);
            if (error != null) {
                req.setAttribute("staff", u);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, creating ? "Them nhan su" : "Sua nhan su");
                return;
            }
            if (creating) {
                service.createUser(u, password);
            } else {
                service.updateUser(u);
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
        u.setRoleId(parsePositiveInt(req.getParameter("roleId")));
        int branchId = parsePositiveInt(req.getParameter("branchId"));
        u.setBranchId(branchId <= 0 ? null : branchId);
        String status = req.getParameter("status");
        u.setStatus(status == null || status.isBlank() ? "ACTIVE" : status);
        return u;
    }

    private String validate(User u, String password, boolean creating) throws Exception {
        if (u.getUsername() == null || u.getUsername().isBlank()) return "Ten dang nhap khong duoc de trong.";
        if (u.getFullName() == null || u.getFullName().isBlank()) return "Ho ten khong duoc de trong.";
        if (u.getEmail() == null || u.getEmail().isBlank()) return "Email khong duoc de trong.";
        if (!u.getEmail().contains("@")) return "Email phai co ky tu @.";
        if (u.getPhone() == null || u.getPhone().isBlank()) return "So dien thoai khong duoc de trong.";
        if (!u.getPhone().matches("^0\\d{9}$")) return "So dien thoai phai gom 10 chu so va bat dau bang 0.";
        if (u.getRoleId() <= 0) return "Vui long chon vai tro.";
        if (u.getBranchId() == null) return "Vui long chon chi nhanh.";
        if (creating && (password == null || password.length() < 6)) return "Mat khau toi thieu 6 ky tu.";
        if (service.usernameTaken(u.getUsername(), u.getUserId())) return "Ten dang nhap da ton tai.";
        if (!"ACTIVE".equals(u.getStatus()) && !"LOCKED".equals(u.getStatus())) return "Trang thai khong hop le.";
        return null;
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

    private int adminRoleId() throws Exception {
        for (Role r : roleService.getRoleList()) {
            if (Constants.ROLE_ADMIN.equals(r.getCode())) return r.getRoleId();
        }
        return -1;
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private void applyLockedFields(User target, User source) {
        target.setUsername(source.getUsername());
        target.setStatus(source.getStatus());
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
