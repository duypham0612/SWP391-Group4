package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import com.cafe.common.IssueReason;
import com.cafe.common.RecountValidator;
import com.cafe.common.RemakeReason;
import com.cafe.model.StockAdjustment;
import com.cafe.model.User;
import com.cafe.service.barista.KdsService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.KdsOrderWorkflowService;
import com.cafe.service.shared.OrderIssueService;
import com.cafe.web.support.BaristaShiftSupport;
import com.cafe.web.support.BaristaWritePolicy;
import com.cafe.web.support.BranchContext;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.RequestParams;
import com.cafe.web.support.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Quầy pha chế: MỘT hàng chờ xếp theo thứ tự pha, kèm dải số liệu bốn trạng thái
 * (chờ pha · đang pha · sẵn sàng · cần xử lý). Không phải bảng ba cột kéo-thả.
 */
@WebServlet("/barista/kds")
public class KdsServlet extends HttpServlet {

    private final KdsService service;
    private final AttendanceService attendance;
    private final BaristaShiftSupport shiftSupport;

    public KdsServlet() {
        this(new KdsService(), new AttendanceService(), new BaristaShiftSupport());
    }

    KdsServlet(KdsService service, AttendanceService attendance,
               BaristaShiftSupport shiftSupport) {
        this.service = Objects.requireNonNull(service, "service");
        this.attendance = Objects.requireNonNull(attendance, "attendance");
        this.shiftSupport = Objects.requireNonNull(shiftSupport, "shiftSupport");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = BranchContext.requireBranchId(req);
        try {
            // Danh sách nguyên liệu của một món — nạp theo yêu cầu khi mở modal "Hết nguyên liệu",
            // thay vì nhúng sẵn vào mọi card (60 card × N nguyên liệu sẽ phình DOM lúc đông khách).
            if ("recipe".equals(req.getParameter("partial"))) {
                Integer productId = RequestParams.optionalInt(req, "productId");
                // Thiếu/sai productId thì trả fragment rỗng để modal hiện lời nhắc,
                // không để NumberFormatException đội lên thành trang lỗi 500.
                req.setAttribute("recipeLines", productId == null
                        ? List.of() : service.getRecipeIngredients(productId));
                req.getRequestDispatcher("/WEB-INF/fragments/barista/kds/ingredient-picker.jsp")
                        .forward(req, resp);
                return;
            }
            if ("depleted".equals(req.getParameter("partial"))) {
                Integer productId = RequestParams.optionalInt(req, "productId");
                req.setAttribute("depletedLines", productId == null
                        ? List.of() : service.getDepletedRecipeIngredients(branchId, productId));
                req.getRequestDispatcher("/WEB-INF/fragments/barista/kds/recount-picker.jsp")
                        .forward(req, resp);
                return;
            }
            loadBoard(req, branchId);
            // Dropdown lý do dựng từ enum để mã + nhãn chỉ khai ở MỘT nơi; blockingCodes cho JS
            // biết lý do nào cần hiện cảnh báo "món sẽ rời hàng chờ".
            req.setAttribute("issueReasons", IssueReason.selectableValues());
            req.setAttribute("remakeReasons", RemakeReason.selectableValues());
            req.setAttribute("blockingReasonCodes", IssueReason.blockingCodesCsv());
            req.setAttribute("pageTitle", "Quầy pha chế");
            boolean partial = "1".equals(req.getParameter("partial"));
            shiftSupport.expose(req, "/barista/kds");
            String view = partial
                ? "/WEB-INF/fragments/barista/kds/cards.jsp"
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
        int branchId = BranchContext.requireBranchId(req);
        if (!BaristaWritePolicy.isKdsAction(action)) {
            rejectInvalidAction(req, resp, branchId);
            return;
        }
        // Chấm công KHÔNG nhận ở màn này — banner ngoài ca chỉ trỏ sang "Ca làm của tôi".
        // Ngoài ca thì chặn ghi, nhưng trả lời bằng ĐÚNG định dạng client đang chờ (fragment khi AJAX)
        // thay vì redirect — xem BaristaShift.blockedOffShift.
        if (shiftSupport.blockedOffShift(req)) {
            if ("1".equals(req.getParameter("ajax"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json;charset=UTF-8");
                resp.setHeader("X-Barista-Write-Denied", "off-shift");
                resp.getWriter().write("{\"error\":\"" + BaristaShiftSupport.OFF_SHIFT_MESSAGE + "\"}");
            } else {
                resp.sendRedirect(req.getContextPath() + "/barista/kds");
            }
            return;
        }
        try {
            dispatch(action, req, userId, branchId, u);
        } catch (NumberFormatException e) {
            // Bắt TRƯỚC IllegalArgumentException (là lớp cha): nếu không, message máy móc kiểu
            // "For input string: ..." của intParam sẽ hiện thẳng lên banner của barista.
            req.getSession().setAttribute("flashError", "Dữ liệu món không hợp lệ. Vui lòng tải lại và thử lại.");
        } catch (IllegalArgumentException | BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", "Không thể cập nhật món lúc này. Vui lòng tải lại và thử lại.");
        }
        // Vẽ lại bảng ở MỌI nhánh — thành công hay lỗi nghiệp vụ đều trả về đúng định dạng
        // client đang chờ, nên chỉ gọi ở một chỗ duy nhất thay vì lặp trong từng khối catch.
        try {
            renderResult(req, resp, branchId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /** Điều phối action đã qua allowlist tới đúng use case. Lỗi để {@link #doPost} bắt tập trung. */
    private void dispatch(String action, HttpServletRequest req, Integer userId, int branchId, User actor)
            throws Exception {
        switch (action) {
            case "start" -> {
                if (!service.startItem(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            case "startOrder" -> startOrder(req, userId, branchId);
            case "markOrderReady" -> markOrderReady(req, userId, branchId);
            case "markReady" -> {
                if (!service.markReady(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            case "reclaim" -> reclaim(req, userId, branchId, actor);
            case "returnQueue" -> {
                if (!service.returnToQueue(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            case "reportIssue" -> reportIssue(req, userId, branchId);
            case "unblock" -> unblock(req, userId, branchId);
            case "remake" -> {
                if (!service.remakeItem(intParam(req, "orderItemId"), remakeReason(req), userId, branchId))
                    flashConflict(req);
                else flashOk(req, "Đã đưa món về hàng chờ với ưu tiên làm lại.");
            }
            default -> { /* BaristaWritePolicy đã chặn trước, nhánh này không tới được. */ }
        }
    }

    /** Cả đơn thường do một người pha trọn; gộp lại để khỏi bấm N lần trên N ly. */
    private void startOrder(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        int claimed = service.startOrder(intParam(req, "orderId"), userId, branchId);
        if (claimed == 0) flashConflict(req);
        else flashOk(req, "Đã nhận pha " + claimed + " món của đơn này.");
    }

    private void markOrderReady(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        KdsOrderWorkflowService.BulkReadyResult result =
                service.markOrderReady(intParam(req, "orderId"), userId, branchId);
        if (result.getCompleted() == 0 && result.getSkippedNoRecipe() == 0) {
            flashConflict(req);
        } else if (result.getSkippedNoRecipe() > 0) {
            // Nói rõ phần chưa xong và lối thoát — món thiếu công thức sẽ không bao giờ tự xong được.
            req.getSession().setAttribute("flashError", "Đã hoàn thành " + result.getCompleted()
                    + " món. Còn " + result.getSkippedNoRecipe()
                    + " món chưa có công thức — hãy bấm Báo sự cố cho từng món đó.");
        } else {
            flashOk(req, "Đã hoàn thành " + result.getCompleted() + " món của đơn này.");
        }
    }

    /**
     * Thu hồi món của người đã rời ca. Điều kiện "đã rời ca" kiểm lại ở SERVER, không tin nút hiện
     * trên màn: bảng có thể đã cũ vài phút và chủ món vừa quay lại quầy.
     */
    private void reclaim(HttpServletRequest req, Integer userId, int branchId, User actor) throws Exception {
        boolean done = service.reclaimItem(intParam(req, "orderItemId"), userId, branchId,
                actor == null ? null : actor.getFullName(), attendance.getOnDutyUserIds(branchId));
        if (!done) flashConflict(req);
        else flashOk(req, "Đã thu hồi món về hàng chờ — ai cũng nhận pha tiếp được.");
    }

    /**
     * Ba nhóm lý do có phạm vi ảnh hưởng khác nhau nên dẫn tới ba hành động khác nhau, thay vì
     * cùng ghi một cờ như trước (khi đó báo sự cố không đổi hành vi hệ thống).
     */
    private void reportIssue(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        IssueReason reason = IssueReason.fromCode(req.getParameter("reason"));
        int itemId = intParam(req, "orderItemId");
        if (reason == IssueReason.OUT_OF_STOCK) {                     // Nhóm A: sửa sổ kho rồi chặn món
            if (!service.blockItemForDepletedIngredients(itemId, ingredientIds(req),
                    issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã ghi hết nguyên liệu vào sổ kho — các món dùng nguyên liệu này "
                    + "tự ẩn khỏi POS/QR, tự hiện lại khi có tồn.");
        } else if (reason != null && reason.isBlocking()) {           // Nhóm B: chặn món
            if (!service.blockItem(itemId, issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã chuyển món sang mục Cần xử lý.");
        } else {                                                      // Nhóm C: chỉ gắn cờ cho Thu ngân
            if (!service.reportIssue(itemId, issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã báo sự cố cho Thu ngân/Quản lý. Món chưa bị hủy.");
        }
    }

    /** Bỏ chặn món; kèm kiểm kê nhanh khi barista khai lại tồn thật cho nguyên liệu vừa có lại. */
    private void unblock(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        int itemId = intParam(req, "orderItemId");
        if (!"1".equals(req.getParameter("recount"))) {
            if (!service.unblockItem(itemId, userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã trả món về hàng chờ.");
            return;
        }
        List<StockAdjustment> recounts = RecountValidator.parse(
                req.getParameterValues("ingredientId"), req.getParameterValues("actualQty"));
        OrderIssueService.UnblockResult result = service.unblockItem(itemId, recounts, userId, branchId);
        if (!result.isSuccess()) {
            flashConflict(req);
        } else if (result.getRemainingBlockedWithRecountedIngredients() > 0) {
            flashOk(req, "Đã trả món về hàng chờ. Còn "
                    + result.getRemainingBlockedWithRecountedIngredients()
                    + " món đang cần xử lý dùng nguyên liệu vừa kiểm lại.");
        } else {
            flashOk(req, "Đã trả món về hàng chờ.");
        }
    }

    private static void flashOk(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flashOk", message);
    }

    private void renderResult(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws Exception {
        if ("1".equals(req.getParameter("ajax"))) {
            loadBoard(req, branchId);
            shiftSupport.expose(req, "/barista/kds");
            req.getRequestDispatcher("/WEB-INF/fragments/barista/kds/cards.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/barista/kds");
        }
    }

    /** KDS AJAX nhận JSON 400 để client hiện lỗi mà không gửi lại form lần thứ hai. */
    private void rejectInvalidAction(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws IOException {
        String message = BaristaWritePolicy.invalidActionMessage();
        req.getSession().setAttribute("flashError", message);
        if ("1".equals(req.getParameter("ajax"))) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("X-Barista-Write-Denied", "invalid-action");
            resp.getWriter().write("{\"error\":\"" + message + "\"}");
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/barista/kds");
    }

    /**
     * Nạp hàng chờ + dải số liệu. Thống kê chính đếm theo SỐ LY (khối lượng việc pha thật), kèm
     * số đơn đang mở làm thông tin phụ. Hàng chờ cắt theo NGÀY KINH DOANH của chi nhánh nên món
     * dang dở của ngày trước không lọt vào — để rác cũ không làm đỏ toàn bộ và lệch số trễ giờ.
     */
    private void loadBoard(HttpServletRequest req, int branchId) throws Exception {
        User current = SessionUtil.currentUser(req);
        Integer currentUserId = current == null ? null : current.getUserId();
        KdsService.KdsBoardQuery query = new KdsService.KdsBoardQuery(
                req.getParameter("owner"), req.getParameter("station"), req.getParameter("orderType"),
                pageParam(req), currentUserId);
        req.setAttribute("board", service.loadBoard(branchId, query));
    }

    /** Trang đang xem; thiếu/không phải số/nhỏ hơn 1 → trang đầu. Vượt trần thì QueuePage kéo về biên. */
    private static int pageParam(HttpServletRequest req) {
        return RequestParams.positiveInt(req, "page", 1);
    }

    /** Món đã bị thao tác khác đổi trạng thái trước — báo cho barista biết bảng vừa được làm mới. */
    private static void flashConflict(HttpServletRequest req) {
        req.getSession().setAttribute("flashError", "Món vừa được cập nhật bởi thao tác khác — bảng đã làm mới.");
    }

    /** Nguyên liệu barista tick là đã hết, lấy từ modal "Hết nguyên liệu". Bỏ qua giá trị rác. */
    private static List<Integer> ingredientIds(HttpServletRequest req) {
        String[] raw = req.getParameterValues("ingredientId");
        List<Integer> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try { out.add(Integer.valueOf(s.trim())); }
            catch (NumberFormatException ignored) { /* tick hỏng → bỏ qua, Service sẽ báo nếu rỗng */ }
        }
        return out;
    }

    /**
     * Chữ lý do sẽ ghi vào nhật ký món. Mã lạ → chuỗi rỗng (Service tự quyết có bắt buộc hay không),
     * riêng OTHER thì lấy nguyên văn barista gõ tay.
     */
    private static String issueReason(HttpServletRequest req) {
        IssueReason reason = IssueReason.fromCode(req.getParameter("reason"));
        if (reason == null) return "";
        return reason == IssueReason.OTHER ? req.getParameter("otherReason") : reason.label();
    }

    private static String remakeReason(HttpServletRequest req) {
        RemakeReason reason = RemakeReason.fromCode(req.getParameter("reason"));
        return reason == null ? "" : reason.label();
    }

    private static int intParam(HttpServletRequest req, String name) {
        return Integer.parseInt(req.getParameter(name));
    }

}
