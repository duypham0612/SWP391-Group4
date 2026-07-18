package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.Constants;
import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.OrderItem;
import com.cafe.model.User;
import com.cafe.service.barista.KdsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Quầy pha chế ba cột: WAITING → MAKING → READY. */
@WebServlet("/barista/kds")
public class KdsServlet extends HttpServlet {

    private final KdsService service = new KdsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            // Danh sách nguyên liệu của một món — nạp theo yêu cầu khi mở modal "Hết nguyên liệu",
            // thay vì nhúng sẵn vào mọi card (60 card × N nguyên liệu sẽ phình DOM lúc đông khách).
            if ("recipe".equals(req.getParameter("partial"))) {
                Integer productId = optionalIntParam(req, "productId");
                // Thiếu/sai productId thì trả fragment rỗng để modal hiện lời nhắc,
                // không để NumberFormatException đội lên thành trang lỗi 500.
                req.setAttribute("recipeLines", productId == null
                        ? java.util.List.of() : service.getRecipeIngredients(productId));
                req.getRequestDispatcher("/WEB-INF/views/barista/_kdsIngredientPicker.jsp").forward(req, resp);
                return;
            }
            loadBoard(req, branchId);
            req.setAttribute("pageTitle", "Quầy pha chế");
            boolean partial = "1".equals(req.getParameter("partial"));
            BaristaShift.expose(req, "/barista/kds");
            String view = partial
                    ? "/WEB-INF/views/barista/kds_cards.jsp"
                    : "/WEB-INF/views/barista/kds.jsp";
            req.getRequestDispatcher(view).forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/kds")) return;   // vào ca / chặn ngoài ca
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("start".equals(action)) {
                if (!service.startItem(intParam(req, "orderItemId"), userId, branchId))
                    flashConflict(req);
            } else if ("markReady".equals(action)) {
                if (!service.markReady(intParam(req, "orderItemId"), userId, branchId))
                    flashConflict(req);
            } else if ("returnQueue".equals(action)) {
                if (!service.returnToQueue(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            } else if ("reportIssue".equals(action)) {
                // Ba nhóm lý do có phạm vi ảnh hưởng khác nhau nên dẫn tới ba hành động khác nhau,
                // thay vì cùng ghi một cờ như trước (khi đó báo sự cố không đổi hành vi hệ thống).
                String code = req.getParameter("reason");
                if ("OUT_OF_STOCK".equals(code)) {                       // Nhóm A: sửa sổ kho rồi chặn món
                    if (!service.blockItemForDepletedIngredients(intParam(req, "orderItemId"),
                            ingredientIds(req), issueReason(req), userId, branchId)) flashConflict(req);
                    else req.getSession().setAttribute("flashOk",
                            "Đã ghi hết nguyên liệu vào sổ kho và chặn món. Xem mục Báo hết món để khoá menu.");
                } else if (BLOCKING_REASONS.contains(code)) {            // Nhóm B: chặn món
                    if (!service.blockItem(intParam(req, "orderItemId"), issueReason(req), userId, branchId))
                        flashConflict(req);
                    else req.getSession().setAttribute("flashOk", "Đã chuyển món sang mục Cần xử lý.");
                } else {                                                 // Nhóm C: chỉ gắn cờ, việc của Thu ngân
                    if (!service.reportIssue(intParam(req, "orderItemId"), issueReason(req), userId, branchId))
                        flashConflict(req);
                    else req.getSession().setAttribute("flashOk", "Đã báo sự cố cho Thu ngân/Quản lý. Món chưa bị hủy.");
                }
            } else if ("unblock".equals(action)) {
                if (!service.unblockItem(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
                else req.getSession().setAttribute("flashOk", "Đã trả món về hàng chờ.");
            } else if ("remake".equals(action)) {
                if (!service.remakeItem(intParam(req, "orderItemId"), remakeReason(req), userId, branchId)) flashConflict(req);
                else req.getSession().setAttribute("flashOk", "Đã đưa món về hàng chờ với ưu tiên làm lại.");
            }
            renderResult(req, resp, branchId);
        } catch (IllegalArgumentException | BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            try { renderResult(req, resp, branchId); }
            catch (SQLException ex) { throw new ServletException(ex); }
        } catch (SQLException e) {
            req.getSession().setAttribute("flashError", "Không thể cập nhật món lúc này. Vui lòng tải lại và thử lại.");
            try { renderResult(req, resp, branchId); }
            catch (SQLException ex) { throw new ServletException(ex); }
        }
    }

    private void renderResult(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws SQLException, ServletException, IOException {
        if ("1".equals(req.getParameter("ajax"))) {
            loadBoard(req, branchId);
            BaristaShift.expose(req, "/barista/kds");
            req.getRequestDispatcher("/WEB-INF/views/barista/kds_cards.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/barista/kds");
        }
    }

    /** Nạp board ba cột và thống kê theo số ly. */
    private void loadBoard(HttpServletRequest req, int branchId) throws SQLException {
        Map<String, List<OrderItem>> board = service.getWorkbenchBoard(branchId);
        List<OrderItem> waiting = board.get("waiting");
        List<OrderItem> inProgress = board.get("inProgress");
        List<OrderItem> ready = board.get("ready");
        List<OrderItem> blocked = board.get("blocked");
        int waitingCups = cups(waiting), progressCups = cups(inProgress), readyCups = cups(ready);
        int overdue = 0;
        OrderItem oldest = null;
        for (OrderItem item : waiting) {
            if (item.getWaitedSeconds() >= Constants.KDS_CRIT_SECONDS) overdue += item.getQuantity();
            if (oldest == null || item.getWaitedSeconds() > oldest.getWaitedSeconds()) oldest = item;
        }
        for (OrderItem item : inProgress) {
            if (item.getWaitedSeconds() >= Constants.KDS_CRIT_SECONDS) overdue += item.getQuantity();
            if (oldest == null || item.getWaitedSeconds() > oldest.getWaitedSeconds()) oldest = item;
        }
        req.setAttribute("waitingItems", waiting);
        req.setAttribute("inProgressItems", inProgress);
        req.setAttribute("readyItems", ready);
        req.setAttribute("blockedItems", blocked);
        req.setAttribute("waitingCount", waitingCups);
        req.setAttribute("makingCount", progressCups);
        req.setAttribute("readyCount", readyCups);
        req.setAttribute("blockedCount", cups(blocked));
        req.setAttribute("overdueCount", overdue);
        req.setAttribute("oldestDisplay", oldest == null ? "—" : (oldest.isOvernight()
                ? "Đơn treo cần xử lý" : OrderItem.formatMinutesLabel(oldest.getWaitedSeconds())));
        req.setAttribute("oldestLocation", oldest == null ? "" : location(oldest));
        req.setAttribute("oldestTier", oldest == null ? "ok" : tier(oldest.getWaitedSeconds()));
        User current = SessionUtil.currentUser(req);
        req.setAttribute("currentUserId", current == null ? 0 : current.getUserId());
        req.setAttribute("kdsWarnMin", Constants.KDS_WARN_SECONDS / 60);
        req.setAttribute("kdsCritMin", Constants.KDS_CRIT_SECONDS / 60);
    }

    private static void flashConflict(HttpServletRequest req) {
        req.getSession().setAttribute("flashError", "Món vừa được cập nhật bởi thao tác khác — bảng đã làm mới.");
    }

    private static int cups(List<OrderItem> items) {
        int total = 0;
        for (OrderItem item : items) total += item.getQuantity();
        return total;
    }

    private static String location(OrderItem item) {
        return item.getTableNumber() == null || item.getTableNumber().isBlank()
                ? "Đơn #" + item.getOrderId() : item.getTableNumber();   // TableNumber đã gồm chữ "Bàn"
    }

    /** Lý do khiến món KHÔNG pha được → chặn món. Các lý do còn lại chỉ cần gắn cờ cho Thu ngân. */
    private static final java.util.Set<String> BLOCKING_REASONS =
            java.util.Set.of("EQUIPMENT", "DISCONTINUED");

    /** Nguyên liệu barista tick là đã hết, lấy từ modal "Hết nguyên liệu". Bỏ qua giá trị rác. */
    private static List<Integer> ingredientIds(HttpServletRequest req) {
        String[] raw = req.getParameterValues("ingredientId");
        List<Integer> out = new java.util.ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try { out.add(Integer.valueOf(s.trim())); }
            catch (NumberFormatException ignored) { /* tick hỏng → bỏ qua, Service sẽ báo nếu rỗng */ }
        }
        return out;
    }

    private static String issueReason(HttpServletRequest req) {
        String selected = req.getParameter("reason");
        if (selected == null || selected.isBlank()) return "";
        if ("OTHER".equals(selected)) return req.getParameter("otherReason");
        Map<String, String> reasons = Map.of(
                "OUT_OF_STOCK", "Hết nguyên liệu",
                "EQUIPMENT", "Máy móc gặp sự cố",
                "NOTE_UNSUPPORTED", "Không đáp ứng được ghi chú",
                "DISCONTINUED", "Món đã ngừng bán",
                "UNCLEAR_ORDER", "Thông tin đơn không rõ");
        return reasons.getOrDefault(selected, "");
    }

    private static String remakeReason(HttpServletRequest req) {
        String selected = req.getParameter("reason");
        if (selected == null || selected.isBlank()) return "";
        Map<String, String> reasons = Map.of(
                "WRONG_RECIPE", "Pha sai công thức",
                "SPILLED", "Làm đổ hoặc hư món",
                "QUALITY", "Chất lượng không đạt",
                "CUSTOMER_FEEDBACK", "Khách phản hồi",
                "WRONG_DELIVERY", "Giao nhầm",
                "CHANGED_REQUEST", "Khách thay đổi yêu cầu");
        return reasons.getOrDefault(selected, "");
    }

    private static int intParam(HttpServletRequest req, String name) {
        return Integer.parseInt(req.getParameter(name));
    }

    /** Như intParam nhưng trả null khi thiếu/không phải số — dùng cho request GET đọc, không ném lỗi. */
    private static Integer optionalIntParam(HttpServletRequest req, String name) {
        String raw = req.getParameter(name);
        if (raw == null || raw.isBlank()) return null;
        try { return Integer.valueOf(raw.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private String tier(int seconds) {
        if (seconds >= Constants.KDS_CRIT_SECONDS) return "crit";
        if (seconds >= Constants.KDS_WARN_SECONDS) return "warn";
        return "ok";
    }
}
