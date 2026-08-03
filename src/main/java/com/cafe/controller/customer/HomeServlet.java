package com.cafe.controller.customer;

import com.cafe.common.Constants;
import com.cafe.model.Branch;
import com.cafe.model.User;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.web.support.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Trang Home công khai → /home. Khách xem thực đơn theo danh mục (ảnh + giá),
 * không cần đăng nhập. Chỉ đọc catalog (không gắn bàn/đặt món).
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private final CatalogReadService catalog;

    public HomeServlet() { this(new CatalogReadService()); }
    HomeServlet(CatalogReadService catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Branch home = catalog.getHomeBranch(resolveBranchId(req));
            req.setAttribute("home", home);
            req.setAttribute("branches", catalog.getPublicHomeBranches());
            req.setAttribute("sections", home == null
                    ? java.util.List.of() : catalog.getPublicMenu(home.getBranchId()));
            req.getRequestDispatcher("/WEB-INF/views/customer/home.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Ngữ cảnh cụ thể ưu tiên request scope/session/query; không có thì DAO fallback active đầu tiên. */
    private Integer resolveBranchId(HttpServletRequest req) {
        Object scoped = req.getAttribute(Constants.ATTR_BRANCH_ID);
        if (scoped instanceof Number number && number.intValue() > 0) return number.intValue();

        User user = SessionUtil.currentUser(req);
        if (user != null && user.getBranchId() != null && user.getBranchId() > 0) return user.getBranchId();

        String requested = req.getParameter("branchId");
        if (requested == null || requested.isBlank()) return null;
        try {
            int branchId = Integer.parseInt(requested.trim());
            return branchId > 0 ? branchId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
