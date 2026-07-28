package com.cafe.controller.admin;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.model.Branch;
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
import java.util.Locale;
import java.util.regex.Pattern;

/** Admin staff accounts. */
@WebServlet("/admin/user")
public class UserServlet extends HttpServlet {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("[a-z][a-z0-9._-]{3,59}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+");

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
                User target = service.getUser(id);
                if (target != null && Constants.ROLE_ADMIN.equals(target.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tài khoản Admin luôn hoạt động, không thể khoá.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
                String to = target != null && "LOCKED".equals(target.getStatus()) ? "ACTIVE" : "LOCKED";
                if ("LOCKED".equals(to) && service.isAssignedBranchManager(id)) {
                    req.getSession().setAttribute("flashError",
                            "Không thể khoá quản lý đang phụ trách chi nhánh.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
                service.setUserStatus(id, to);
                req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái nhân sự.");
                resp.sendRedirect(ctx + "/admin/user");
                return;
            }
            User u = bind(req);
            String password = req.getParameter("password");
            boolean creating = u.getUserId() == 0;
            int assignmentBranchId = parsePositiveInt(req.getParameter("assignmentBranchId"));
            boolean assignmentMode = creating && assignmentBranchId > 0;
            if (assignmentMode) {
                Branch assignmentBranch = branchService.getBranch(assignmentBranchId);
                if (assignmentBranch == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if (assignmentBranch.getManagerUserId() != null) {
                    req.getSession().setAttribute("flashError", "Chi nhánh đã có quản lý phụ trách.");
                    resp.sendRedirect(ctx + "/admin/branch");
                    return;
                }
                Role managerRole = roleByCode(Constants.ROLE_MANAGER);
                u.setRoleId(managerRole.getRoleId());
                u.setBranchId(assignmentBranchId);
                u.setBranchName(assignmentBranch.getName());
                setAssignmentAttributes(req, assignmentBranch, managerRole);
            }

            if (creating && u.getRoleId() == adminRoleId()) {
                req.setAttribute("staff", u);
                req.setAttribute("errorMsg", "Hệ thống chỉ có 1 Admin toàn chuỗi, không thể tạo thêm tài khoản Admin.");
                forwardForm(req, resp, "Thêm nhân sự");
                return;
            }
            User existing = null;
            if (!creating) {
                existing = service.getUser(u.getUserId());
                if (existing == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if (Constants.ROLE_ADMIN.equals(existing.getRoleCode())) {
                    req.getSession().setAttribute("flashError", "Tài khoản Admin hệ thống không thể chỉnh sửa.");
                    resp.sendRedirect(ctx + "/admin/user");
                    return;
                }
                applyLockedFields(u, existing);
            }

            String error = validate(u, password, creating, existing);
            if (error != null) {
                req.setAttribute("staff", u);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, creating ? "Thêm nhân sự" : "Sửa nhân sự");
                return;
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
        } catch (Exception e) { throw new ServletException(e); }
    }

    private User bind(HttpServletRequest req) {
        User u = new User();
        String id = req.getParameter("userId");
        if (id != null && !id.isBlank()) u.setUserId(Integer.parseInt(id));
        u.setUsername(normalizeUsername(req.getParameter("username")));
        u.setFullName(trim(req.getParameter("fullName")));
        u.setEmail(normalizeEmail(req.getParameter("email")));
        u.setPhone(trim(req.getParameter("phone")));
        u.setRoleId(parsePositiveInt(req.getParameter("roleId")));
        int branchId = parsePositiveInt(req.getParameter("branchId"));
        u.setBranchId(branchId <= 0 ? null : branchId);
        String status = req.getParameter("status");
        u.setStatus(status == null || status.isBlank() ? "ACTIVE" : status);
        return u;
    }

    private String validate(User u, String password, boolean creating, User existing) throws Exception {
        if (u.getUsername() == null || u.getUsername().isBlank()) return "Tên đăng nhập không được để trống.";
        if (u.getUsername().length() < 4 || u.getUsername().length() > 60)
            return "Tên đăng nhập phải có từ 4 đến 60 ký tự.";
        if (!USERNAME_PATTERN.matcher(u.getUsername()).matches())
            return "Tên đăng nhập phải bắt đầu bằng chữ cái và chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.";
        if (u.getFullName() == null || u.getFullName().isBlank()) return "Họ tên không được để trống.";
        if (u.getEmail() == null || u.getEmail().isBlank()) return "Email không được để trống.";
        if (u.getEmail().length() > 120 || !EMAIL_PATTERN.matcher(u.getEmail()).matches())
            return "Email không đúng định dạng, ví dụ: ten@congty.vn.";
        if (u.getPhone() == null || u.getPhone().isBlank()) return "Số điện thoại không được để trống.";
        if (!u.getPhone().matches("^0\\d{9}$")) return "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.";
        if (u.getRoleId() <= 0) return "Vui lòng chọn vai trò.";
        if (u.getBranchId() == null) return "Vui lòng chọn chi nhánh.";
        if (creating && (password == null || password.length() < 6)) return "Mật khẩu tối thiểu 6 ký tự.";
        if (service.usernameTaken(u.getUsername(), u.getUserId())) return "Tên đăng nhập đã tồn tại.";
        if (isChanged(u.getEmail(), existing == null ? null : existing.getEmail())
                && service.emailTaken(u.getEmail(), u.getUserId()))
            return "Email đã được sử dụng bởi nhân sự khác.";
        if (isChanged(u.getPhone(), existing == null ? null : existing.getPhone())
                && service.phoneTaken(u.getPhone(), u.getUserId()))
            return "Số điện thoại đã được sử dụng bởi nhân sự khác.";
        if (service.isBranchManagerRole(u.getRoleId())
                && service.branchHasOtherManager(u.getBranchId(), u.getUserId()))
            return "Chi nhánh đã có quản lý phụ trách.";
        if (!"ACTIVE".equals(u.getStatus()) && !"LOCKED".equals(u.getStatus())) return "Trạng thái không hợp lệ.";
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
        Role role = roleByCode(Constants.ROLE_ADMIN);
        return role == null ? -1 : role.getRoleId();
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

    private String normalizeUsername(String value) {
        String username = trim(value);
        return username == null ? null : username.toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String email = trim(value);
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    private boolean isChanged(String value, String existingValue) {
        return existingValue == null || !existingValue.equalsIgnoreCase(value);
    }

    private void applyLockedFields(User target, User source) {
        target.setUsername(source.getUsername());
        target.setStatus(source.getStatus());
        target.setBranchId(source.getBranchId());
        target.setBranchName(source.getBranchName());
    }

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
