package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import com.cafe.common.Constants;
import com.cafe.web.support.CsrfUtil;
import com.cafe.common.Reason86;
import com.cafe.web.support.SessionUtil;
import com.cafe.web.support.BaristaShiftSupport;
import com.cafe.web.support.BaristaWritePolicy;
import com.cafe.model.User;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.web.support.BranchContext;
import com.cafe.web.support.RequestParams;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** B3 · EightySixServlet → /barista/eightysix. Bật/tắt hết món (khoá khỏi POS + QR). */
@WebServlet("/barista/eightysix")
public class EightySixServlet extends HttpServlet {

    private final BranchMenuService service;
    private final BaristaShiftSupport shiftSupport;
    private static final DateTimeFormatter HTML_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    public EightySixServlet() { this(new BranchMenuService(), new BaristaShiftSupport()); }
    EightySixServlet(BranchMenuService service, BaristaShiftSupport shiftSupport) {
        this.service = Objects.requireNonNull(service);
        this.shiftSupport = Objects.requireNonNull(shiftSupport);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = BranchContext.requireBranchId(req);
        String query = RequestParams.text(req, "q", 100);
        String state = normalizeState(req.getParameter("state"));
        int pageSize = normalizePageSize(RequestParams.positiveInt(req, "pageSize", 10));
        try {
            BranchMenuService.MenuAvailabilityPage menuPage = service.getMenuAvailabilityPage(
                    branchId, query, state, RequestParams.positiveInt(req, "page", 1), pageSize);
            req.setAttribute("menuPage", menuPage);
            req.setAttribute("items", menuPage.getItems());
            req.setAttribute("filterQuery", query);
            req.setAttribute("filterState", state);
            req.setAttribute("suggest86", service.getSuggested86(branchId));   // gợi ý 86 (soft): nguyên liệu đã cạn
            req.setAttribute("openRequests", service.getOpenRequestsMap(branchId));
            req.setAttribute("reasons", Reason86.selectableValues());   // chỉ nhóm "sự cố" — kho tự lo phần hết tồn
            LocalDateTime now = LocalDateTime.now();
            req.setAttribute("etaMin", now.plusMinutes(Constants.MENU86_ETA_MIN_MINUTES).format(HTML_DT));
            req.setAttribute("etaMax", now.plusDays(Constants.MENU86_ETA_MAX_DAYS).format(HTML_DT));
            req.setAttribute("pageTitle", "Báo hết món");
            shiftSupport.expose(req, "/barista/eightysix");   // trực ca: banner + khoá thao tác khi ngoài ca
            req.getRequestDispatcher("/WEB-INF/views/barista/eightysix.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String action = req.getParameter("action");
        String redirect = returnUrl(req);
        if (!BaristaWritePolicy.isEightySixAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(redirect);
            return;
        }
        if (shiftSupport.guardWrite(req, resp, "/barista/eightysix")) return;   // ngoài ca → chặn ghi
        int branchId = BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
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

    static String normalizeState(String value) {
        if ("available".equalsIgnoreCase(value)) return "available";
        if ("out".equalsIgnoreCase(value)) return "out";
        return "";
    }

    static int normalizePageSize(int value) {
        return value == 20 || value == 50 ? value : 10;
    }

    private static String returnUrl(HttpServletRequest req) {
        StringBuilder url = new StringBuilder(req.getContextPath()).append("/barista/eightysix");
        appendQueryParam(url, "q", RequestParams.text(req, "q", 100));
        appendQueryParam(url, "state", normalizeState(req.getParameter("state")));
        int page = RequestParams.positiveInt(req, "page", 1);
        if (page > 1) appendQueryParam(url, "page", String.valueOf(page));
        int pageSize = normalizePageSize(RequestParams.positiveInt(req, "pageSize", 10));
        if (pageSize != 10) appendQueryParam(url, "pageSize", String.valueOf(pageSize));
        return url.toString();
    }

    private static void appendQueryParam(StringBuilder url, String name, String value) {
        if (value == null || value.isBlank()) return;
        url.append(url.indexOf("?") >= 0 ? '&' : '?')
                .append(name).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
