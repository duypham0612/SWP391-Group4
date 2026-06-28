package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Đặt branchId của user đang đăng nhập vào request attribute để tầng dưới lọc dữ liệu.
 * ADMIN có branchId = null (toàn chuỗi). Chạy sau AuthFilter/RbacFilter.
 */
public class BranchScopeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        User u = (User) req.getSession().getAttribute(Constants.SESSION_USER);
        if (u != null) {
            req.setAttribute(Constants.ATTR_BRANCH_ID, u.getBranchId());
            CsrfUtil.getToken(req); // đảm bảo session đăng nhập luôn có CSRF token cho form ghi
        }
        chain.doFilter(request, response);
    }
}
