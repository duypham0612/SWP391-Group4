package com.cafe.controller.auth;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * A0 · Auth Gateway (đặc tả mục 4). Đăng nhập bằng HttpSession (không JWT).
 * Routes: GET/POST /auth/login, GET /auth/logout. Actions: showLogin / login / logout.
 */
@WebServlet({"/auth/login", "/auth/logout", "/auth/forgot"})
public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (path.equals("/auth/logout")) { logout(req, resp); return; }
        if (path.equals("/auth/forgot")) { showForgot(req, resp); return; }
        showLogin(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (path.equals("/auth/login")) {
            login(req, resp);
        } else if (path.equals("/auth/forgot")) {
            forgot(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void showLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (SessionUtil.isLoggedIn(req)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        CsrfUtil.getToken(req);
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    private void login(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            fail(req, resp, "Phiên không hợp lệ, vui lòng thử lại.");
            return;
        }
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            User u = authService.authenticate(username, password);
            if (u == null) {
                fail(req, resp, "Sai tên đăng nhập hoặc mật khẩu.");
                return;
            }
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();           // chống session fixation
            HttpSession session = req.getSession(true);
            session.setAttribute(Constants.SESSION_USER, u);
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (Exception e) {
            fail(req, resp, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void showForgot(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (SessionUtil.isLoggedIn(req)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        CsrfUtil.getToken(req);
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
    }

    private void forgot(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { forgotFail(req, resp, "Phiên không hợp lệ, vui lòng thử lại."); return; }
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String pwd = req.getParameter("newPassword");
        String confirm = req.getParameter("confirmPassword");
        if (pwd == null || pwd.length() < 6) { forgotFail(req, resp, "Mật khẩu mới tối thiểu 6 ký tự."); return; }
        if (!pwd.equals(confirm)) { forgotFail(req, resp, "Xác nhận mật khẩu không khớp."); return; }
        try {
            boolean ok = authService.resetPasswordSelfService(username, email, pwd);
            if (!ok) { forgotFail(req, resp, "Tên đăng nhập và email không khớp tài khoản hợp lệ."); return; }
            req.getSession().setAttribute("flashOk", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
            resp.sendRedirect(req.getContextPath() + "/auth/login");
        } catch (Exception e) {
            forgotFail(req, resp, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void forgotFail(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        CsrfUtil.getToken(req);
        req.setAttribute("errorMsg", msg);
        req.setAttribute("username", req.getParameter("username"));
        req.setAttribute("email", req.getParameter("email"));
        req.getRequestDispatcher("/WEB-INF/views/auth/forgot.jsp").forward(req, resp);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String msg)
            throws ServletException, IOException {
        CsrfUtil.getToken(req);
        req.setAttribute("errorMsg", msg);
        req.setAttribute("username", req.getParameter("username"));
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
