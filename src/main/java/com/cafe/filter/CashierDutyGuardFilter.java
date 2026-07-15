package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.User;
import com.cafe.service.cashier.CashierDutyService;
import com.cafe.service.cashier.CashierDutyService.DutyState;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Chặn thao tác ghi của thu ngân khi chưa bắt đầu ca trực. */
public class CashierDutyGuardFilter implements Filter {

    private static final String MESSAGE = "Bạn cần bắt đầu ca trước khi thao tác.";

    private final CashierDutyService dutyService = new CashierDutyService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        User user = SessionUtil.currentUser(req);
        if (user == null || !Constants.ROLE_CASHIER.equals(user.getRoleCode())) {
            chain.doFilter(request, response);
            return;
        }

        String ctx = req.getContextPath();
        String path = req.getRequestURI().substring(ctx.length());
        DutyState state;
        try {
            state = dutyService.getDutyState(user.getUserId(), InventoryDashboardServlet.branchId(req));
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        req.setAttribute("cashierDutyState", state.name());
        req.setAttribute("cashierOnDuty", state == DutyState.ON_DUTY);

        if (isAllowed(path, req.getMethod()) || state == DutyState.ON_DUTY) {
            chain.doFilter(request, response);
            return;
        }

        if (wantsJson(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"error\":\"" + MESSAGE + "\"}");
            return;
        }

        req.getSession().setAttribute("flashError", MESSAGE);
        resp.sendRedirect(ctx + "/cashier/shift");
    }

    private boolean isAllowed(String path, String method) {
        return "GET".equalsIgnoreCase(method)
                || path.equals("/cashier/shift")
                || path.equals("/cashier/dashboard");
    }

    private boolean wantsJson(HttpServletRequest req) {
        String contentType = req.getContentType();
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        return (contentType != null && contentType.toLowerCase().contains("application/json"))
                || (accept != null && accept.toLowerCase().contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
