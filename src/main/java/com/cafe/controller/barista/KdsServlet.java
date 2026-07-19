package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

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

    /** Số đơn treo hiển thị tối đa trong drawer cảnh báo — phần dư trỏ về màn Quản lý. */
    private static final int STALE_GROUP_LIMIT = 6;

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
                // Vị trí đặt: chỉ nhận giá trị trong whitelist, sai/rỗng → null (không tin client).
                String loc = req.getParameter("handoverLocation");
                if (loc != null && !com.cafe.common.Constants.HANDOVER_LOCATIONS.contains(loc)) loc = null;
                if (!service.markReady(intParam(req, "orderItemId"), userId, branchId, loc))
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

    /**
     * Nạp board ba cột. Thống kê chính đếm theo SỐ LY (khối lượng việc pha thật), kèm số dòng
     * món và số đơn làm thông tin phụ. Đơn của ngày kinh doanh trước KHÔNG vào hàng chờ mà
     * nằm ở khu "Đơn treo cần xử lý" — để rác cũ không làm đỏ toàn bộ và lệch số trễ giờ.
     */
    private void loadBoard(HttpServletRequest req, int branchId) throws SQLException {
        com.cafe.model.Branch branch = service.getBranch(branchId);
        java.time.LocalTime openTime = branch == null ? null : branch.getOpenTime();
        int branchPeakThreshold = branch == null ? 0 : branch.getPeakThresholdCups();
        java.time.LocalDateTime dayStart = com.cafe.common.BusinessDay.startUtc(openTime);
        Map<String, List<OrderItem>> board = service.getWorkbenchBoard(branchId, dayStart);
        List<OrderItem> waiting = board.get("waiting");
        List<OrderItem> inProgress = board.get("inProgress");
        List<OrderItem> ready = board.get("ready");
        List<OrderItem> blocked = board.get("blocked");
        List<OrderItem> stale = service.getStaleItems(branchId, dayStart);

        int overdue = 0;
        OrderItem oldest = null;
        int totalPrepSeconds = 0;
        java.util.Set<Integer> activeBaristas = new java.util.HashSet<>();
        for (List<OrderItem> bucket : List.of(waiting, inProgress)) {
            for (OrderItem item : bucket) {
                if (item.getSlaTier().equals("late")) overdue += item.getQuantity();
                if (oldest == null || item.getWaitedSeconds() > oldest.getWaitedSeconds()) oldest = item;
                totalPrepSeconds += item.getPrepSeconds() * item.getQuantity();
                if (item.getBaristaId() != null) activeBaristas.add(item.getBaristaId());
            }
        }

        // Chế độ cao điểm: đo bằng số ly đang chờ+đang pha so ngưỡng chi nhánh.
        int queueCups = cups(waiting) + cups(inProgress);
        boolean peakMode = com.cafe.service.barista.KdsService.isPeak(queueCups, branchPeakThreshold);
        int estLastSeconds = com.cafe.service.barista.KdsService
                .estimateLastWaitSeconds(totalPrepSeconds, activeBaristas.size());
        // Số thứ tự pha cho hàng chờ (chỉ dùng khi cao điểm) — theo đúng thứ tự FIFO đã sắp.
        int seq = 1;
        for (OrderItem item : waiting) item.setSeqNo(seq++);
        req.setAttribute("peakMode", peakMode);
        req.setAttribute("peakQueueCups", queueCups);
        req.setAttribute("peakEstLastMin", (estLastSeconds + 59) / 60);

        req.setAttribute("waitingItems", waiting);
        req.setAttribute("inProgressItems", inProgress);
        req.setAttribute("readyItems", ready);
        req.setAttribute("blockedItems", blocked);
        // Đơn treo gộp theo đơn và giới hạn số dòng: barista không thao tác được trên chúng,
        // đổ hết ra chỉ che mất hàng chờ thật. Phần dư trỏ về Quản lý xử lý.
        List<StaleOrderGroup> staleGroups = StaleOrderGroup.from(stale);
        req.setAttribute("staleOrderCount", staleGroups.size());
        req.setAttribute("staleHasItems", !staleGroups.isEmpty());
        req.setAttribute("staleHiddenOrders", Math.max(0, staleGroups.size() - STALE_GROUP_LIMIT));
        req.setAttribute("staleGroups", staleGroups.size() > STALE_GROUP_LIMIT
                ? staleGroups.subList(0, STALE_GROUP_LIMIT) : staleGroups);
        req.setAttribute("waitingCount", cups(waiting));
        req.setAttribute("makingCount", cups(inProgress));
        req.setAttribute("readyCount", cups(ready));
        req.setAttribute("blockedCount", cups(blocked));
        req.setAttribute("staleCount", cups(stale));
        req.setAttribute("overdueCount", overdue);
        // Thông tin phụ: số dòng món và số đơn đang mở của cả board.
        req.setAttribute("waitingLines", waiting.size());
        req.setAttribute("makingLines", inProgress.size());
        req.setAttribute("readyLines", ready.size());
        // Tính cả đơn bị chặn: khách vẫn đang ngồi đợi nên đơn vẫn đang mở —
        // bỏ ra thì con số này mâu thuẫn với chính chữ ở khu "Cần xử lý".
        req.setAttribute("openOrderCount", distinctOrders(waiting, inProgress, ready, blocked));

        // Thanh "Chờ lâu nhất": phút + bàn + tên món cụ thể, không còn chuỗi mơ hồ.
        req.setAttribute("oldestDisplay", oldest == null ? "—"
                : OrderItem.formatMinutesLabel(oldest.getWaitedSeconds()));
        req.setAttribute("oldestLocation", oldest == null ? "" : location(oldest));
        req.setAttribute("oldestProduct", oldest == null ? "" : oldest.getProductName());
        req.setAttribute("oldestQty", oldest == null ? 0 : oldest.getQuantity());
        // Màu thanh "Chờ lâu nhất" khớp đúng màu card của chính món đó (cùng từ vựng tier).
        req.setAttribute("oldestTier", oldest == null ? "ok" : oldest.getSlaTier());

        User current = SessionUtil.currentUser(req);
        req.setAttribute("currentUserId", current == null ? 0 : current.getUserId());
        req.setAttribute("handoverLocations", com.cafe.common.Constants.HANDOVER_LOCATIONS);
    }

    @SafeVarargs
    private static int distinctOrders(List<OrderItem>... buckets) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (List<OrderItem> bucket : buckets) for (OrderItem it : bucket) ids.add(it.getOrderId());
        return ids.size();
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
}
