package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.common.Reason86;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.shared.BranchMenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** B3 · EightySixServlet → /barista/eightysix. Bật/tắt hết món (khoá khỏi POS + QR). */
@WebServlet("/barista/eightysix")
public class EightySixServlet extends HttpServlet {

    private final BranchMenuService service = new BranchMenuService();
    private static final DateTimeFormatter HTML_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("items", service.getMenuAvailability(branchId));
            req.setAttribute("suggest86", service.getSuggested86(branchId));   // gợi ý 86 (soft): nguyên liệu đã cạn
            req.setAttribute("openRequests", service.getOpenRequestsMap(branchId));
            req.setAttribute("reasons", Reason86.selectableValues());   // chỉ nhóm "sự cố" — kho tự lo phần hết tồn
            LocalDateTime now = LocalDateTime.now();
            req.setAttribute("etaMin", now.plusMinutes(Constants.MENU86_ETA_MIN_MINUTES).format(HTML_DT));
            req.setAttribute("etaMax", now.plusDays(Constants.MENU86_ETA_MAX_DAYS).format(HTML_DT));
            req.setAttribute("pageTitle", "Báo hết món");
            BaristaShift.expose(req, "/barista/eightysix");   // trực ca: banner + khoá thao tác khi ngoài ca
            req.getRequestDispatcher("/WEB-INF/views/barista/eightysix.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/eightysix")) return;   // vào ca / chặn ngoài ca
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        String redirect = req.getContextPath() + "/barista/eightysix";
        try {
            if ("report86".equals(action)) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                LocalDateTime eta = parseEta(req.getParameter("backInEta"));
                service.request86(branchId, productId,
                        req.getParameter("reasonCode"), req.getParameter("note"), eta, userId);
                req.getSession().setAttribute("flashOk", "Đã báo tạm hết món, chờ quản lý xử lý.");
            } else if ("askReopen".equals(action)) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                service.requestReopen(branchId, productId, userId);
                req.getSession().setAttribute("flashOk", "Đã gửi yêu cầu, chờ quản lý duyệt.");
            }
            resp.sendRedirect(redirect);
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (DateTimeParseException e) {
            req.getSession().setAttribute("flashError", "Định dạng thời gian không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã món không hợp lệ.");
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private LocalDateTime parseEta(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return LocalDateTime.parse(raw);
    }
}
