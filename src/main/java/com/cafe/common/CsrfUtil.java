package com.cafe.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/** Sinh & kiểm tra CSRF token theo session. Form ghi (POST) phải gửi field _csrf. */
public final class CsrfUtil {

    private static final SecureRandom RND = new SecureRandom();

    private CsrfUtil() { }

    /** Lấy token hiện tại, tạo mới nếu chưa có. */
    public static String getToken(HttpServletRequest req) {
        HttpSession s = req.getSession();
        Object t = s.getAttribute(Constants.SESSION_CSRF);
        if (t == null) {
            byte[] b = new byte[24];
            RND.nextBytes(b);
            t = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            s.setAttribute(Constants.SESSION_CSRF, t);
        }
        return (String) t;
    }

    public static boolean isValid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return false;
        Object t = s.getAttribute(Constants.SESSION_CSRF);
        String form = req.getParameter("_csrf");
        return t != null && t.equals(form);
    }
}
