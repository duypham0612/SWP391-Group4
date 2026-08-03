package com.cafe.web.support;

import com.cafe.common.Constants;
import com.cafe.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Theo dõi session đăng nhập để Admin có thể thu hồi quyền ngay sau thay đổi bảo mật. */
public final class ActiveSessionRegistry
        implements HttpSessionListener, HttpSessionAttributeListener {

    private static final ConcurrentHashMap<Integer, Set<HttpSession>> SESSIONS_BY_USER =
            new ConcurrentHashMap<>();

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if (Constants.SESSION_USER.equals(event.getName()) && event.getValue() instanceof User user) {
            register(user.getUserId(), event.getSession());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        if (!Constants.SESSION_USER.equals(event.getName())) return;
        if (event.getValue() instanceof User previous) {
            unregister(previous.getUserId(), event.getSession());
        }
        Object current = event.getSession().getAttribute(Constants.SESSION_USER);
        if (current instanceof User user) register(user.getUserId(), event.getSession());
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if (Constants.SESSION_USER.equals(event.getName()) && event.getValue() instanceof User user) {
            unregister(user.getUserId(), event.getSession());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        SESSIONS_BY_USER.forEach((userId, sessions) -> sessions.remove(session));
        SESSIONS_BY_USER.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static int invalidateUserSessions(int userId) {
        Set<HttpSession> sessions = SESSIONS_BY_USER.remove(userId);
        if (sessions == null) return 0;
        int invalidated = 0;
        for (HttpSession session : sessions) {
            try {
                session.invalidate();
                invalidated++;
            } catch (IllegalStateException ignored) {
                // Session đã hết hạn hoặc đã đăng xuất đồng thời.
            }
        }
        return invalidated;
    }

    private static void register(int userId, HttpSession session) {
        if (userId <= 0) return;
        SESSIONS_BY_USER.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    private static void unregister(int userId, HttpSession session) {
        Set<HttpSession> sessions = SESSIONS_BY_USER.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) SESSIONS_BY_USER.remove(userId, sessions);
    }
}
