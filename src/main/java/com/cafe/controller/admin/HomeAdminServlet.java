package com.cafe.controller.admin;

import com.cafe.web.support.CsrfUtil;
import com.cafe.model.Branch;
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

    private final HomeAdminService service;

    public HomeAdminServlet() { this(new HomeAdminService()); }
    HomeAdminServlet(HomeAdminService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Integer branchId = positiveInt(req.getParameter("branchId"));
            req.setAttribute("products", service.getProductsForAdmin());
            req.setAttribute("branches", service.getBranches());
            req.setAttribute("setting", service.getHomeBranch(branchId));
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
                Branch s = new Branch();
                Integer branchId = positiveInt(req.getParameter("branchId"));
                s.setBranchId(branchId == null ? 0 : branchId);
                s.setHeroEyebrow(trim(req.getParameter("heroEyebrow")));
                s.setHeroTitle(trim(req.getParameter("heroTitle")));
                s.setHeroSubtitle(trim(req.getParameter("heroSubtitle")));
                s.setHeroImageUrl(trim(req.getParameter("heroImageUrl")));
                String error = validateContent(s);
                if (error != null) {
                    req.getSession().setAttribute("flashError", error);
                } else {
                    service.saveContent(s);
                    req.getSession().setAttribute("flashOk", "Đã lưu nội dung trang Home.");
                }
            }
            Integer branchId = positiveInt(req.getParameter("branchId"));
            resp.sendRedirect(ctx + "/admin/home" + (branchId == null ? "" : "?branchId=" + branchId));
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void saveHomeProducts(HttpServletRequest req) throws Exception {
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
        req.getSession().setAttribute("flashOk", "Đã lưu hiển thị và thứ tự các món trên Home.");
    }

    private String validateContent(Branch s) {
        if (s.getBranchId() <= 0)
            return "Vui lòng chọn chi nhánh cần cập nhật hero.";
        if (s.getHeroTitle() == null || s.getHeroTitle().isBlank())
            return "Tiêu đề trang Home không được để trống.";
        return null;
    }

    private int intParam(HttpServletRequest req, String name, int def) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private String trim(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private Integer positiveInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
