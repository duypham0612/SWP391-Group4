package com.cafe.filter;

import com.cafe.common.SessionUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Chặn truy cập khi chưa đăng nhập. Đường dẫn public được whitelist;
 * còn lại bắt buộc có session user, nếu không -> redirect /login.
 * Khai báo & xếp thứ tự trong web.xml (chạy sau CharsetFilter, trước RbacFilter).
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();
        String path = req.getRequestURI().substring(ctx.length());

        if (isPublic(path) || SessionUtil.isLoggedIn(req)) {
            chain.doFilter(request, response);
            return;
        }
        resp.sendRedirect(ctx + "/auth/login");
    }

    private boolean isPublic(String path) {
        return path.equals("/")
            || path.equals("/home")
            || path.equals("/auth/login")
            || path.equals("/auth/logout")
            || path.equals("/auth/forgot")
            || path.equals("/health")
            || path.startsWith("/assets/")
            || path.startsWith("/qr/");   // QR app khách (Phase 6) — không cần đăng nhập
    }
}
