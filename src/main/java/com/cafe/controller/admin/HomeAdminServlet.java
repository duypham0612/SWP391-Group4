package com.cafe.controller.admin;

import com.cafe.common.BusinessException;
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
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/home");
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
                service.saveContent(s);
                req.getSession().setAttribute("flashOk", "Đã lưu nội dung trang Home.");
            } else {
                throw new BusinessException("Thao tác cập nhật trang Home không hợp lệ.");
            }
            Integer branchId = positiveInt(req.getParameter("branchId"));
            resp.sendRedirect(ctx + "/admin/home" + (branchId == null ? "" : "?branchId=" + branchId));
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            Integer branchId = positiveInt(req.getParameter("branchId"));
            resp.sendRedirect(ctx + "/admin/home" + (branchId == null ? "" : "?branchId=" + branchId));
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void saveHomeProducts(HttpServletRequest req) throws Exception {
        String[] pids = req.getParameterValues("pid");
        if (pids == null || pids.length == 0) {
            throw new BusinessException("Không tìm thấy danh sách sản phẩm cần lưu.");
        }
        if (pids.length > HomeAdminService.MAX_HOME_PRODUCTS) {
            throw new BusinessException(
                    "Mỗi lần chỉ được lưu tối đa " + HomeAdminService.MAX_HOME_PRODUCTS + " sản phẩm.");
        }
        java.util.List<Integer> idList = new java.util.ArrayList<>();
        java.util.List<Boolean> showList = new java.util.ArrayList<>();
        java.util.List<Integer> orderList = new java.util.ArrayList<>();
        for (String raw : pids) {
            int pid = requiredPositiveInt(raw, "Mã sản phẩm không hợp lệ.");
            idList.add(pid);
            showList.add(req.getParameter("show_" + pid) != null);
            orderList.add(requiredHomeOrder(req.getParameter("order_" + pid)));
        }
        int n = idList.size();
        int[] ids = new int[n];
        boolean[] shows = new boolean[n];
        int[] orders = new int[n];
        int shownCount = 0;
        for (int i = 0; i < n; i++) {
            ids[i] = idList.get(i);
            shows[i] = showList.get(i);
            orders[i] = orderList.get(i);
            if (shows[i]) shownCount++;
        }
        service.saveProductHomeBatch(ids, shows, orders);
        req.getSession().setAttribute("flashOk",
                "Đã lưu " + n + " sản phẩm; " + shownCount + " sản phẩm đang hiển thị trên Home.");
    }

    private int requiredPositiveInt(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(message);
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException(message);
        }
    }

    private int requiredHomeOrder(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0 || parsed > HomeAdminService.MAX_HOME_SORT_ORDER) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new BusinessException("Thứ tự hiển thị phải là số nguyên từ 0 đến "
                    + HomeAdminService.MAX_HOME_SORT_ORDER + ".");
        }
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
