package com.cafe.controller.barista;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Bàn giao ca: gửi đầu việc tới ca sau, nhận bàn giao và theo dõi việc tồn. */
@WebServlet("/barista/handover")
public class ShiftHandoverServlet extends HttpServlet {
    private static final String SELF = "/barista/handover";
    private final HandoverService service = new HandoverService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req);
        try {
            // Màn này đã có summary.pendingForMe riêng; không lặp thêm truy vấn đếm của cảnh báo dùng chung.
            BaristaShift.expose(req, MyShiftServlet.PATH, false);
            int userId = user == null ? 0 : user.getUserId();
            boolean onShift = Boolean.TRUE.equals(req.getAttribute("onShift"));
            String scope = scopeParam(req), status = statusParam(req), query = textParam(req, "q", 100);
            int pageSize = pageSizeParam(req);
            HandoverService.HandoverBoard board = service.loadBoard(branchId, userId, onShift,
                    scope, status, query, positiveIntParam(req, "page", 1), pageSize);

            req.setAttribute("handoverPage", board.getPage());
            req.setAttribute("summary", board.getSummary());
            req.setAttribute("receiverPreview", board.getReceiver());
            req.setAttribute("receiverPreviewError", board.getReceiverError());
            req.setAttribute("handoverRequired", board.isHandoverRequired());
            // Hai quyền tách nhau: ca quá hạn chấm công vẫn lập được bàn giao nhưng không tan ca được.
            req.setAttribute("canCreateHandover", board.isCanCreate());
            req.setAttribute("canClockOutHandover", board.isCanClockOut());
            req.setAttribute("carryOverTasks", board.getCarryOverTasks());
            req.setAttribute("brewTasks", board.getBrewTasks());
            req.setAttribute("filterScope", scope);
            req.setAttribute("filterStatus", status);
            req.setAttribute("filterQuery", query);
            req.setAttribute("expiredPrepBatchCount", service.countExpiredActivePrepBatches(branchId));
            req.setAttribute("pageTitle", "Bàn giao ca");
            req.getRequestDispatcher("/WEB-INF/views/barista/handover.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req); int userId = user == null ? 0 : user.getUserId();
        String action = req.getParameter("action");
        // Quay lại đúng bộ lọc + trang đang xem; thiếu nó là mỗi lần đổi trạng thái một việc lại văng về trang 1.
        String redirect = selfUrlKeepingFilters(req, null);
        if (!BaristaWritePolicy.isHandoverAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(req.getContextPath() + redirect);
            return;
        }
        try {
            if ("create".equals(action) || "createAndClockOut".equals(action)) {
                // Không chặn theo onShift ở đây: ca quá hạn chấm công vẫn phải giao được việc tồn.
                // Service mới là nơi kiểm — có ca đang mở không, và có còn tan ca được không.
                List<String> tasks = Arrays.asList(req.getParameterValues("task") == null ? new String[0] : req.getParameterValues("task"));
                if ("createAndClockOut".equals(action)) {
                    service.createHandoverAndClockOut(branchId, userId, req.getParameter("note"), tasks);
                    redirect = MyShiftServlet.PATH;   // tan ca xong xem lại thẻ chấm công và giờ công
                } else {
                    int id = service.createHandover(branchId, userId, req.getParameter("note"), tasks);
                    redirect = selfUrlKeepingFilters(req, id);
                }
                req.getSession().setAttribute("flashOk", "Đã gửi bàn giao" + ("createAndClockOut".equals(action) ? " và tan ca." : "."));
            } else if ("acknowledge".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn cần vào ca trước khi nhận bàn giao.");
                int handoverId = parsePositive(req, "handoverId");
                service.acknowledge(branchId, handoverId, userId);
                redirect = selfUrlKeepingFilters(req, handoverId);
                req.getSession().setAttribute("flashOk", "Đã xác nhận nhận bàn giao.");
            } else if ("claim".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn cần vào ca trước khi tiếp nhận bàn giao.");
                int handoverId = parsePositive(req, "handoverId");
                service.claim(branchId, handoverId, userId);
                redirect = selfUrlKeepingFilters(req, handoverId);
                req.getSession().setAttribute("flashOk", "Đã tiếp nhận bàn giao của ca trước.");
            } else if ("updateTask".equals(action)) {
                if (!BaristaShift.onShift(req)) throw new IllegalStateException("Bạn cần đang trong ca để cập nhật việc bàn giao.");
                int handoverId = parsePositive(req, "handoverId");
                service.updateTaskStatus(branchId, handoverId, parsePositive(req, "taskId"), req.getParameter("status"), userId);
                redirect = selfUrlKeepingFilters(req, handoverId);
                req.getSession().setAttribute("flashOk", "Đã cập nhật việc bàn giao.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) { req.getSession().setAttribute("flashError", e.getMessage()); redirect = selfUrlKeepingFilters(req, null); }
        catch (Exception e) { throw new ServletException(e); }
        resp.sendRedirect(req.getContextPath() + redirect);
    }

    private static int parsePositive(HttpServletRequest req, String name) { try { int value = Integer.parseInt(req.getParameter(name)); if (value > 0) return value; } catch (NumberFormatException ignored) {} throw new IllegalArgumentException("Dữ liệu bàn giao không hợp lệ."); }

    private static String scopeParam(HttpServletRequest req) { return allowedParam(req, "scope", "MINE", "SENT"); }

    /**
     * Tên "state" chứ không phải "status": form đổi trạng thái từng việc bên dưới đã dùng "status",
     * trùng tên là mỗi lần bấm "Đang xử lý" cho một việc thì cả danh sách bị lọc theo giá trị đó.
     */
    private static String statusParam(HttpServletRequest req) { return allowedParam(req, "state", "WAITING_RECEIPT", "IN_PROGRESS", "COMPLETED"); }

    private static String allowedParam(HttpServletRequest req, String name, String... allowed) {
        String value = textParam(req, name, 20).toUpperCase();
        for (String item : allowed) if (item.equals(value)) return value;
        return "";
    }

    private static String textParam(HttpServletRequest req, String name, int maxLength) {
        String raw = req.getParameter(name);
        if (raw == null) return "";
        String value = raw.trim();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private static int positiveIntParam(HttpServletRequest req, String name, int fallback) {
        try { int value = Integer.parseInt(req.getParameter(name)); return value > 0 ? value : fallback; }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(positiveIntParam(req, "pageSize", 5));
    }

    /** Chỉ nhận đúng các mức có trên giao diện; giá trị lạ (kể cả rất lớn) rơi về mặc định. */
    static int normalizePageSize(int value) {
        return value == 10 || value == 20 || value == 50 ? value : 5;
    }

    private static String selfUrlKeepingFilters(HttpServletRequest req, Integer focusHandoverId) {
        return buildSelfUrl(textParam(req, "q", 100), scopeParam(req), statusParam(req),
                pageSizeParam(req), positiveIntParam(req, "page", 1), focusHandoverId);
    }

    /**
     * URL quay lại màn này kèm bộ lọc + trang đang xem (PRG). Phần thuần của
     * {@link #selfUrlKeepingFilters} — tách ra để test được mà không cần dựng request.
     * {@code focusHandoverId} thêm neo tới đúng thẻ vừa thao tác: danh sách dài mà nhảy về đầu trang
     * thì barista phải cuộn tìm lại chỗ cũ sau mỗi lần đổi trạng thái một việc.
     */
    static String buildSelfUrl(String query, String scope, String status, int pageSize, int page, Integer focusHandoverId) {
        StringBuilder url = new StringBuilder(SELF);
        appendParam(url, "q", query);
        appendParam(url, "scope", scope);
        appendParam(url, "state", status);
        appendParam(url, "pageSize", String.valueOf(pageSize));
        appendParam(url, "page", String.valueOf(page));
        if (focusHandoverId != null && focusHandoverId > 0) url.append("#h").append(focusHandoverId);
        return url.toString();
    }

    private static void appendParam(StringBuilder url, String name, String value) {
        if (value == null || value.isEmpty()) return;
        url.append(url.indexOf("?") < 0 ? '?' : '&').append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
