package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchStatusDao;
import com.cafe.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Đặt branchId của user đang đăng nhập vào request attribute để tầng dưới lọc dữ liệu.
 * ADMIN có branchId = null (toàn chuỗi). Chạy sau AuthFilter/RbacFilter.
 */
public class BranchScopeFilter implements Filter {

    private static final String BRANCH_STOPPED_MESSAGE =
            "Chi nhánh đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.";

    private final BranchStatusDao branchStatusDao = new BranchStatusDao();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        User u = (User) req.getSession().getAttribute(Constants.SESSION_USER);
        if (u != null) {
            if (u.getBranchId() != null && requiresActiveBranch(req) && !isBranchActive(u.getBranchId())) {
                HttpSession session = req.getSession(false);
                if (session != null) session.invalidate();
                req.getSession(true).setAttribute("flashError", BRANCH_STOPPED_MESSAGE);
                resp.sendRedirect(req.getContextPath() + "/auth/login");
                return;
            }
            req.setAttribute(Constants.ATTR_BRANCH_ID, u.getBranchId());
            CsrfUtil.getToken(req); // đảm bảo session đăng nhập luôn có CSRF token cho form ghi
        }
        chain.doFilter(request, response);
    }

    private boolean isBranchActive(int branchId) throws ServletException {
        try (Connection conn = DBConnection.getConnection()) {
            return branchStatusDao.isActive(conn, branchId);
        } catch (SQLException e) {
            throw new ServletException("Không thể kiểm tra trạng thái chi nhánh.", e);
        }
    }

    private boolean requiresActiveBranch(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        return !path.startsWith("/assets/")
                && !path.startsWith("/auth/")
                && !path.equals("/")
                && !path.equals("/home")
                && !path.equals("/health")
                && !path.startsWith("/qr/");
    }
}
