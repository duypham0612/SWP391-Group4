package com.cafe.web.support;

import com.cafe.common.Constants;
import com.cafe.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Tiện ích đọc người dùng đang đăng nhập từ session. */
public final class SessionUtil {

    private SessionUtil() { }

    public static User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute(Constants.SESSION_USER);
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return currentUser(req) != null;
    }

}
