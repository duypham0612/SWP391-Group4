package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.HomeSetting;
import com.cafe.service.admin.HomeAdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin editor for public Home content. */
@WebServlet("/admin/home")
public class HomeAdminServlet extends HttpServlet {

    private final HomeAdminService service = new HomeAdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("products", service.getProductsForAdmin());
            req.setAttribute("setting", service.getHomeSetting());
            req.setAttribute("pageTitle", "Trang Home");
            req.getRequestDispatcher("/WEB-INF/views/admin/home-editor.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("saveHomeProducts".equals(action)) {
                saveHomeProducts(req);
            } else if ("saveContent".equals(action)) {
                HomeSetting s = new HomeSetting();
                s.setHeroEyebrow(trim(req.getParameter("heroEyebrow")));
                s.setHeroTitle(trim(req.getParameter("heroTitle")));
                s.setHeroSubtitle(trim(req.getParameter("heroSubtitle")));
                s.setHeroImageUrl(trim(req.getParameter("heroImageUrl")));
                String error = validateContent(s);
                if (error != null) {
                    req.getSession().setAttribute("flashError", error);
                } else {
                    service.saveContent(s);
                    req.getSession().setAttribute("flashOk", "Da luu noi dung trang Home.");
                }
            }
            resp.sendRedirect(ctx + "/admin/home");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void saveHomeProducts(HttpServletRequest req) throws java.sql.SQLException {
        String[] pids = req.getParameterValues("pid");
        if (pids == null || pids.length == 0) return;
        java.util.List<Integer> idList = new java.util.ArrayList<>();
        java.util.List<Boolean> showList = new java.util.ArrayList<>();
        java.util.List<Integer> orderList = new java.util.ArrayList<>();
        for (String raw : pids) {
            if (raw == null || raw.isBlank()) continue;
            int pid;
            try { pid = Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { continue; }
            idList.add(pid);
            showList.add(req.getParameter("show_" + pid) != null);
            orderList.add(intParam(req, "order_" + pid, 0));
        }
        int n = idList.size();
        if (n == 0) return;
        int[] ids = new int[n];
        boolean[] shows = new boolean[n];
        int[] orders = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = idList.get(i);
            shows[i] = showList.get(i);
            orders[i] = orderList.get(i);
        }
        service.saveProductHomeBatch(ids, shows, orders);
        req.getSession().setAttribute("flashOk", "Da luu hien thi va thu tu cac mon tren Home.");
    }

    private String validateContent(HomeSetting s) {
        if (s.getHeroTitle() == null || s.getHeroTitle().isBlank())
            return "Tieu de trang Home khong duoc de trong.";
        return null;
    }

    private int intParam(HttpServletRequest req, String name, int def) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
