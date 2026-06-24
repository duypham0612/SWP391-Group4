package com.mycoffee.filter;

import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * AuthFilter — Bộ lọc phân quyền trung tâm của hệ thống.
 *
 * LUẬT PHÂN QUYỀN:
 *   - Chưa đăng nhập → chuyển về /login (trừ các trang công khai)
 *   - RoleID = 4 (Customer) cố truy cập route Admin/Staff → 403 Forbidden
 *
 * Ánh xạ RoleID từ bảng Roles:
 *   1 = Admin | 2 = Branch Manager | 3 = Employee | 4 = Customer
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    // ──────────────────────────────────────────────────────────────
    // DANH SÁCH CÁC ĐƯỜNG DẪN CÔNG KHAI (không cần đăng nhập)
    // ──────────────────────────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
        "/login",
        "/register",
        "/login.jsp",
        "/register.jsp",
        "/index.jsp",
        "/menu",
        "/customer-menu",
        "/product-detail",
        "/customer-qr-order",
        "/customer-cart",
        "/customer-checkout",
        "/customer-order-status"
    };

    // ──────────────────────────────────────────────────────────────
    // ROUTE CHỈ DÀNH CHO STAFF (Admin/Manager/Employee) — RoleID 1,2,3
    // Customer (RoleID=4) sẽ bị chặn 403 nếu cố vào
    // ──────────────────────────────────────────────────────────────
    private static final String[] STAFF_ONLY_PREFIXES = {
        // ── Admin ──────────────────────────────────────────────────
        "/admin-dashboard",
        // ── Manager ────────────────────────────────────────────────
        "/manager-dashboard",
        "/manager-tables",
        "/manager-attendance",
        "/manager-inventory",
        "/manager-report",
        // ── Cashier / Employee ─────────────────────────────────────
        "/pos",          // khớp /pos và /pos-tables (/pos bắt đầu bằng /pos)
        "/pos-tables",
        "/table-layout",
        "/kitchen",
        "/cashier",
        // ── Trang JSP trực tiếp (phòng thủ thêm lớp) ──────────────
        "/views/admin",
        "/views/manager",
        // ── Quản lý nội dung ───────────────────────────────────────
        "/inventory",
        "/report",
        "/employee",
        "/shift",
        "/attendance",
        "/branch",
        "/product-manage",
        "/voucher-manage"
    };

    // ──────────────────────────────────────────────────────────────
    // ROUTE CHỈ DÀNH CHO CUSTOMER (RoleID=4)
    // Staff sẽ bị chặn nếu cố dùng tính năng "tại bàn" của khách
    // (Hiện tại chưa cần chặn cứng — Staff có thể xem menu để hỗ trợ khách)
    // ──────────────────────────────────────────────────────────────

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Không cần khởi tạo gì thêm
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String contextPath = request.getContextPath();          // VD: /mycoffee
        String requestURI  = request.getRequestURI();           // VD: /mycoffee/admin-dashboard
        // Lấy phần path sau context để so sánh
        String path = requestURI.substring(contextPath.length());

        // 1. Bỏ qua các tài nguyên tĩnh (CSS, JS, hình ảnh...)
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Cho phép các URL công khai đi thẳng qua mà không cần kiểm tra
        if (isPublicUrl(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 3. Kiểm tra xem người dùng đã đăng nhập chưa
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            // Chưa đăng nhập → về trang login
            response.sendRedirect(contextPath + "/login");
            return;
        }

        int roleId = user.getRoleId();

        // 4. Customer không được truy cập route quản trị/nhân viên.
        if (roleId == User.ROLE_CUSTOMER && isStaffOnlyRoute(path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Truy cập bị từ chối! Bạn không có quyền truy cập trang này. (403 Forbidden)");
            return;
        }

        // 5. Tất cả kiểm tra đã qua → cho tiếp tục
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Không cần dọn dẹp gì
    }

    // ──────────────────────────────────────────────────────────────
    // PHƯƠNG THỨC HỖ TRỢ (Helper Methods)
    // ──────────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem path có phải tài nguyên tĩnh không (CSS, JS, hình ảnh...).
     * Các tài nguyên tĩnh luôn được cho qua để tránh ảnh hưởng hiệu suất.
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/fonts/")
            || path.startsWith("/assets/")
            || path.endsWith(".css")
            || path.endsWith(".js")
            || path.endsWith(".png")
            || path.endsWith(".jpg")
            || path.endsWith(".jpeg")
            || path.endsWith(".gif")
            || path.endsWith(".svg")
            || path.endsWith(".ico")
            || path.endsWith(".woff")
            || path.endsWith(".woff2");
    }

    /**
     * Kiểm tra xem path có nằm trong danh sách URL công khai không.
     */
    private boolean isPublicUrl(String path) {
        for (String publicUrl : PUBLIC_URLS) {
            if (path.equals(publicUrl) || path.startsWith(publicUrl + "?")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem path có phải là route chỉ dành cho Staff (RoleID 1,2,3) không.
     * Nếu Customer (RoleID=4) cố truy cập thì trả về true để bị chặn 403.
     */
    private boolean isStaffOnlyRoute(String path) {
        for (String prefix : STAFF_ONLY_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + "?")) {
                return true;
            }
        }
        return false;
    }
}
