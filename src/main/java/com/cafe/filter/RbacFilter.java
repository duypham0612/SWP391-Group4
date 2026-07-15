package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Phân quyền theo prefix đường dẫn vs role. ADMIN được truy cập mọi nơi.
 * Chạy sau AuthFilter (đã đảm bảo có user). Sai quyền -> 403.
 */
public class RbacFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String ctx = req.getContextPath();
        String path = req.getRequestURI().substring(ctx.length());

        String required = requiredRole(path);
        if (required == null) {           // không thuộc vùng giới hạn role
            chain.doFilter(request, response);
            return;
        }

        User u = (User) req.getSession().getAttribute(Constants.SESSION_USER);
        if (u != null && (Constants.ROLE_ADMIN.equals(u.getRoleCode()) || required.equals(u.getRoleCode()))) {
            chain.doFilter(request, response);
            return;
        }
        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private String requiredRole(String path) {
        if (path.startsWith("/admin/"))   return Constants.ROLE_ADMIN;
        if (path.startsWith("/manager/")) return Constants.ROLE_MANAGER;
        if (path.startsWith("/cashier/")) return Constants.ROLE_CASHIER;
        if (path.startsWith("/barista/")) return Constants.ROLE_BARISTA;
        return null;
    }
}
