package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.web.support.CsrfUtil;
import com.cafe.common.BranchAccessPolicy;
import com.cafe.model.User;
import com.cafe.service.shared.BranchAccessService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Objects;

/**
 * Đặt branchId của user đang đăng nhập vào request attribute để tầng dưới lọc dữ liệu.
 * ADMIN có branchId = null (toàn chuỗi). Chạy sau AuthFilter/RbacFilter.
 */
public class BranchScopeFilter implements Filter {

    private final BranchAccessService branchAccessService;

    public BranchScopeFilter() {
        this(new BranchAccessService());
    }

    BranchScopeFilter(BranchAccessService branchAccessService) {
        this.branchAccessService = Objects.requireNonNull(branchAccessService, "branchAccessService");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        User u = (User) req.getSession().getAttribute(Constants.SESSION_USER);
        if (u != null) {
            if (u.getBranchId() != null && requiresActiveBranch(req)) {
                BranchAccessService.Status status = getBranchAccessStatus(u.getBranchId());
                String blockedMessage = BranchAccessPolicy.blockedMessage(
                        status.active(), status.managerAssigned());
                if (blockedMessage != null) {
                    HttpSession session = req.getSession(false);
                    if (session != null) session.invalidate();
                    req.getSession(true).setAttribute("flashError", blockedMessage);
                    resp.sendRedirect(req.getContextPath() + "/auth/login");
                    return;
                }
            }
            req.setAttribute(Constants.ATTR_BRANCH_ID, u.getBranchId());
            CsrfUtil.getToken(req); // đảm bảo session đăng nhập luôn có CSRF token cho form ghi
        }
        chain.doFilter(request, response);
    }

    private BranchAccessService.Status getBranchAccessStatus(int branchId)
            throws ServletException {
        try {
            return branchAccessService.getStatus(branchId);
        } catch (Exception e) {
            throw new ServletException("Không thể kiểm tra quyền hoạt động của chi nhánh.", e);
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
