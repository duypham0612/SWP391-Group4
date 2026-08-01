package com.cafe.controller.shared;

import com.cafe.web.support.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Điểm vào duy nhất của ứng dụng localhost. */
@WebServlet("/start")
public final class RootServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = SessionUtil.currentUser(request) == null ? "/home" : "/dashboard";
        response.sendRedirect(request.getContextPath() + target);
    }
}
