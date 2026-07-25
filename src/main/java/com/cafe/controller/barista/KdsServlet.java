package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.RecountValidator;
import com.cafe.common.SessionUtil;
import com.cafe.model.OrderItem;
import com.cafe.model.StockAdjustment;
import com.cafe.model.User;
import com.cafe.service.barista.KdsService;
import com.cafe.service.shared.OrderService;
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
            if ("depleted".equals(req.getParameter("partial"))) {
                Integer productId = optionalIntParam(req, "productId");
                req.setAttribute("depletedLines", productId == null
                        ? java.util.List.of() : service.getDepletedRecipeIngredients(branchId, productId));
                req.getRequestDispatcher("/WEB-INF/views/barista/_kdsRecountPicker.jsp").forward(req, resp);
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
        // Vào ca / tan ca vẫn đi lối redirect thường (nút chấm công nằm ngoài khung bảng, post bình thường).
        String clockRedirect = BaristaShift.handleClock(req, action, "/barista/kds");
        if (clockRedirect != null) { resp.sendRedirect(req.getContextPath() + clockRedirect); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        // Ngoài ca thì chặn ghi, nhưng trả lời bằng ĐÚNG định dạng client đang chờ (fragment khi AJAX)
        // thay vì redirect — xem BaristaShift.blockedOffShift.
        if (BaristaShift.blockedOffShift(req)) {
            try { renderResult(req, resp, branchId); }
            catch (SQLException e) { throw new ServletException(e); }
            return;
        }
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
                            "Đã ghi hết nguyên liệu vào sổ kho — các món dùng nguyên liệu này tự ẩn khỏi POS/QR, tự hiện lại khi có tồn.");
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
                if ("1".equals(req.getParameter("recount"))) {
                    List<StockAdjustment> recounts = RecountValidator.parse(
                            req.getParameterValues("ingredientId"), req.getParameterValues("actualQty"));
                    OrderService.UnblockResult result =
                            service.unblockItem(intParam(req, "orderItemId"), recounts, userId, branchId);
                    if (!result.isSuccess()) flashConflict(req);
                    else if (result.getRemainingBlockedWithRecountedIngredients() > 0) {
                        req.getSession().setAttribute("flashOk", "Đã trả món về hàng chờ. Còn "
                                + result.getRemainingBlockedWithRecountedIngredients()
                                + " món đang cần xử lý dùng nguyên liệu vừa kiểm lại.");
                    } else req.getSession().setAttribute("flashOk", "Đã trả món về hàng chờ.");
                } else {
                    if (!service.unblockItem(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
                    else req.getSession().setAttribute("flashOk", "Đã trả món về hàng chờ.");
                }
            } else if ("remake".equals(action)) {
                if (!service.remakeItem(intParam(req, "orderItemId"), remakeReason(req), userId, branchId)) flashConflict(req);
                else req.getSession().setAttribute("flashOk", "Đã đưa món về hàng chờ với ưu tiên làm lại.");
            }
            renderResult(req, resp, branchId);
        } catch (NumberFormatException e) {
            // Bắt TRƯỚC IllegalArgumentException (là lớp cha): nếu không, message máy móc kiểu
            // "For input string: ..." của intParam sẽ hiện thẳng lên banner của barista.
            req.getSession().setAttribute("flashError", "Dữ liệu món không hợp lệ. Vui lòng tải lại và thử lại.");
            try { renderResult(req, resp, branchId); }
            catch (SQLException ex) { throw new ServletException(ex); }
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
        // Một truy vấn duy nhất: danh sách phẳng đã đúng thứ tự pha; các con số chỉ phân giỏ lại
        // trên chính danh sách đó.
        List<OrderItem> queue = service.getWorkbenchQueue(branchId, dayStart);
        Map<String, List<OrderItem>> board = com.cafe.service.barista.KdsService.splitWorkbench(queue);
        List<OrderItem> waiting = board.get("waiting");
        List<OrderItem> inProgress = board.get("inProgress");
        List<OrderItem> ready = board.get("ready");
        List<OrderItem> blocked = board.get("blocked");

        // Chế độ cao điểm: đo bằng SỐ LY đang chờ+đang pha so ngưỡng chi nhánh — thuần khối lượng
        // việc, không dính đồng hồ. Màn này cố ý không hiển thị bất kỳ số liệu thời gian nào:
        // thứ tự trong danh sách đã là thứ tự pha, đặt trước thì nằm trên.
        int queueCups = cups(waiting) + cups(inProgress);
        boolean peakMode = com.cafe.service.barista.KdsService.isPeak(queueCups, branchPeakThreshold);
        // Danh sách một cột + số thứ tự pha. Món đã pha xong dồn xuống cuối và không đánh số:
        // với barista chúng không còn là việc, xen giữa thì việc thật bị đẩy khuất xuống dưới.
        List<OrderItem> queueItems = pourOrder(queue);
        int seq = 1;
        for (OrderItem item : queueItems) {
            if (!"READY".equals(item.getStatus())) item.setSeqNo(seq++);
        }
        req.setAttribute("peakMode", peakMode);
        req.setAttribute("peakQueueCups", queueCups);

        User current = SessionUtil.currentUser(req);
        Integer currentUserId = current == null ? null : current.getUserId();
        // Lọc rồi mới cắt trang: có vậy "x/y món" và số trang mới đếm trên đúng tập đang xem.
        // Số thứ tự pha đã gán ở trên nên vẫn là vị trí thật trong cả hàng chờ, không đánh lại.
        String owner = filterParam(req, "owner", OWNER_FILTERS);
        String station = filterParam(req, "station", STATION_FILTERS);
        String orderType = filterParam(req, "orderType", ORDER_TYPE_FILTERS);
        List<OrderItem> visible = com.cafe.service.barista.KdsService.filterWorkbench(
                queueItems, owner, station, orderType, currentUserId);
        req.setAttribute("queueTotal", queueItems.size());
        req.setAttribute("queuePage", com.cafe.service.barista.KdsService.paginate(
                visible, pageParam(req), QUEUE_PAGE_SIZE));
        req.setAttribute("filterOwner", owner);
        req.setAttribute("filterStation", station);
        req.setAttribute("filterOrderType", orderType);

        req.setAttribute("waitingItems", waiting);
        req.setAttribute("inProgressItems", inProgress);
        req.setAttribute("readyItems", ready);
        req.setAttribute("blockedItems", blocked);
        req.setAttribute("waitingCount", cups(waiting));
        req.setAttribute("makingCount", cups(inProgress));
        req.setAttribute("readyCount", cups(ready));
        req.setAttribute("blockedCount", cups(blocked));
        // Thông tin phụ: số dòng món và số đơn đang mở của cả board.
        req.setAttribute("waitingLines", waiting.size());
        req.setAttribute("makingLines", inProgress.size());
        req.setAttribute("readyLines", ready.size());
        // Tính cả đơn bị chặn: khách vẫn đang ngồi đợi nên đơn vẫn đang mở —
        // bỏ ra thì con số này mâu thuẫn với chính chữ ở khu "Cần xử lý".
        req.setAttribute("openOrderCount", distinctOrders(waiting, inProgress, ready, blocked));

        // Đơn treo của ngày kinh doanh TRƯỚC. Chúng bị cắt khỏi hàng chờ chính để rác cũ không
        // làm lệch thống kê, nhưng phải hiện ở một khu riêng — bỏ hẳn thì món nằm ngoài MỌI màn
        // của barista, khách vẫn đang chờ mà không ai ở quầy biết là còn nợ ly đó.
        // Không lọc và không phân trang: khu này vốn phải rỗng, có dòng nào là phải xử lý dòng đó.
        List<OrderItem> stale = service.getStaleItems(branchId, dayStart);
        req.setAttribute("staleItems", stale);
        req.setAttribute("staleCups", cups(stale));

        req.setAttribute("currentUserId", currentUserId == null ? 0 : currentUserId);
        req.setAttribute("handoverLocations", com.cafe.common.Constants.HANDOVER_LOCATIONS);
    }

    /**
     * Số dòng mỗi trang. Chọn 12 để một trang vừa khít khung hàng chờ trên màn quầy phổ thông
     * mà không phải cuộn — barista liếc một lần là thấy trọn việc của trang.
     */
    private static final int QUEUE_PAGE_SIZE = 12;

    private static final java.util.Set<String> OWNER_FILTERS = java.util.Set.of("all", "mine", "unassigned");
    private static final java.util.Set<String> STATION_FILTERS = java.util.Set.of("all", "COFFEE", "TEA", "BLENDER");
    private static final java.util.Set<String> ORDER_TYPE_FILTERS =
            java.util.Set.of("all", "DINE_IN", "TAKEAWAY", "DELIVERY");

    /** Bộ lọc từ client chỉ được nhận nếu nằm trong whitelist; giá trị lạ coi như không lọc. */
    private static String filterParam(HttpServletRequest req, String name, java.util.Set<String> allowed) {
        String raw = req.getParameter(name);
        return raw != null && allowed.contains(raw) ? raw : "all";
    }

    /** Trang đang xem; thiếu/không phải số/nhỏ hơn 1 → trang đầu. Vượt trần thì QueuePage kéo về biên. */
    private static int pageParam(HttpServletRequest req) {
        Integer value = optionalIntParam(req, "page");
        return value == null || value < 1 ? 1 : value;
    }

    /**
     * Thứ tự danh sách một cột: việc còn phải làm (chờ pha · đang pha · cần xử lý) giữ nguyên
     * thứ tự pha do truy vấn trả về (làm lại trước, rồi FIFO theo giờ đặt); món ĐÃ pha xong dồn
     * xuống cuối vì chúng chỉ còn chờ người giao, không phải việc của quầy.
     */
    private static List<OrderItem> pourOrder(List<OrderItem> queue) {
        List<OrderItem> out = new java.util.ArrayList<>(queue.size());
        for (OrderItem it : queue) if (!"READY".equals(it.getStatus())) out.add(it);
        for (OrderItem it : queue) if ("READY".equals(it.getStatus())) out.add(it);
        return out;
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
