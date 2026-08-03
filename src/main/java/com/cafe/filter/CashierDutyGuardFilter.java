package com.cafe.filter;

import com.cafe.common.Constants;
import com.cafe.web.support.SessionUtil;
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
import java.util.Objects;

/** Chặn thao tác ghi của thu ngân khi chưa bắt đầu ca trực. */
public class CashierDutyGuardFilter implements Filter {

    private static final String MESSAGE = "Bạn cần bắt đầu ca trước khi thao tác.";
    private static final String DUTY_DENIED_HEADER = "X-Cashier-Duty-Denied";
    private static final String DUTY_REDIRECT_HEADER = "X-Cashier-Duty-Redirect";

    private final CashierDutyService dutyService;

    public CashierDutyGuardFilter() {
        this(new CashierDutyService());
    }

    CashierDutyGuardFilter(CashierDutyService dutyService) {
        this.dutyService = Objects.requireNonNull(dutyService, "dutyService");
    }

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
            state = dutyService.getDutyState(user.getUserId(), com.cafe.web.support.BranchContext.requireBranchId(req));
        } catch (Exception e) {
            throw new ServletException(e);
        }

        req.setAttribute("cashierDutyState", state.name());
        req.setAttribute("cashierOnDuty", state == DutyState.ON_DUTY);

        if (isAllowed(path, req.getMethod()) || state == DutyState.ON_DUTY) {
            chain.doFilter(request, response);
            return;
        }

        if (isAsyncRequest(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader(DUTY_DENIED_HEADER, "true");
            resp.setHeader(DUTY_REDIRECT_HEADER, ctx + "/cashier/shift");
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

    private boolean isAsyncRequest(HttpServletRequest req) {
        String contentType = req.getContentType();
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        return (contentType != null && contentType.toLowerCase().contains("application/json"))
                || (accept != null && accept.toLowerCase().contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || "1".equals(req.getParameter("ajax"));
    }
}
