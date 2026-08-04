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
            if ("recipe".equals(req.getParameter("partial"))) {
                Integer productId = RequestParams.optionalInt(req, "productId");
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
            req.getSession().setAttribute("flashError", "Dữ liệu món không hợp lệ. Vui lòng tải lại và thử lại.");
        } catch (IllegalArgumentException | BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            req.getSession().setAttribute("flashError", "Không thể cập nhật món lúc này. Vui lòng tải lại và thử lại.");
        }
        try {
            renderResult(req, resp, branchId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void dispatch(String action, HttpServletRequest req, Integer userId, int branchId, User actor)
            throws Exception {
        switch (action) {
            // EN: "Nhận pha" button on queue-row.jsp. Claims 1 item WAITING->MAKING. -> KdsService.startItem()
            case "start" -> {
                if (!service.startItem(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            // EN: "Nhận pha cả đơn" button on queue-row.jsp. Claims all WAITING items of an order at once. -> startOrder() below
            case "startOrder" -> startOrder(req, userId, branchId);
            // EN: "Xong cả đơn" button on queue-row.jsp. Completes all MY MAKING items of an order. -> markOrderReady() below
            case "markOrderReady" -> markOrderReady(req, userId, branchId);
            // EN: "Xong" button on queue-row.jsp. Completes 1 item MAKING->READY + auto stock deduction. -> KdsService.markReady()
            case "markReady" -> {
                if (!service.markReady(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            // EN: "Thu hồi món" button on queue-row.jsp. Rescues an item stuck under an off-shift owner. -> reclaim() below
            case "reclaim" -> reclaim(req, userId, branchId, actor);
            // EN: "Trả lại chờ" button on queue-row.jsp. Owner voluntarily releases item MAKING->WAITING. -> KdsService.returnToQueue()
            case "returnQueue" -> {
                if (!service.returnToQueue(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            // EN: "Báo sự cố" modal on kds.jsp (#issueModal). Reports a problem with 1 item. -> reportIssue() below
            case "reportIssue" -> reportIssue(req, userId, branchId);
            // EN: "Trả về chờ pha" button/modal on queue-row.jsp / kds.jsp (#unblockModal). Clears BLOCKED->WAITING. -> unblock() below
            case "unblock" -> unblock(req, userId, branchId);
            // EN: "Làm lại" modal on kds.jsp (#remakeModal). Redoes item, back to WAITING with priority. -> KdsService.remakeItem()
            case "remake" -> {
                if (!service.remakeItem(intParam(req, "orderItemId"), remakeReason(req), userId, branchId))
                    flashConflict(req);
                else flashOk(req, "Đã đưa món về hàng chờ với ưu tiên làm lại.");
            }
            // EN: unreachable — BaristaWritePolicy already rejected unknown actions before dispatch() is called.
            default -> { }
        }
    }

    // EN: Builds the flash message for a bulk claim; claimed==0 means every item was already taken. -> KdsService.startOrder()
    private void startOrder(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        int claimed = service.startOrder(intParam(req, "orderId"), userId, branchId);
        if (claimed == 0) flashConflict(req);
        else flashOk(req, "Đã nhận pha " + claimed + " món của đơn này.");
    }

    // EN: Builds the flash message for bulk complete; shows a warning when some items lack a recipe. -> KdsService.markOrderReady()
    private void markOrderReady(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        KdsOrderWorkflowService.BulkReadyResult result =
                service.markOrderReady(intParam(req, "orderId"), userId, branchId);
        if (result.getCompleted() == 0 && result.getSkippedNoRecipe() == 0) {
            flashConflict(req);
        } else if (result.getSkippedNoRecipe() > 0) {
            req.getSession().setAttribute("flashError", "Đã hoàn thành " + result.getCompleted()
                    + " món. Còn " + result.getSkippedNoRecipe()
                    + " món chưa có công thức — hãy bấm Báo sự cố cho từng món đó.");
        } else {
            flashOk(req, "Đã hoàn thành " + result.getCompleted() + " món của đơn này.");
        }
    }

    // EN: Reads current on-duty barista IDs to pass to the service, which re-checks the off-shift condition. -> KdsService.reclaimItem()
    private void reclaim(HttpServletRequest req, Integer userId, int branchId, User actor) throws Exception {
        boolean done = service.reclaimItem(intParam(req, "orderItemId"), userId, branchId,
                actor == null ? null : actor.getFullName(), attendance.getOnDutyUserIds(branchId));
        if (!done) flashConflict(req);
        else flashOk(req, "Đã thu hồi món về hàng chờ — ai cũng nhận pha tiếp được.");
    }

    // EN: Reads reason code from the modal form, then routes to 1-of-3 issue-report use cases below.
    private void reportIssue(HttpServletRequest req, Integer userId, int branchId) throws Exception {
        IssueReason reason = IssueReason.fromCode(req.getParameter("reason"));
        int itemId = intParam(req, "orderItemId");
        // EN: Out-of-stock case. Zeroes the chosen ingredients' stock AND blocks this item. -> KdsService.blockItemForDepletedIngredients()
        if (reason == IssueReason.OUT_OF_STOCK) {
            if (!service.blockItemForDepletedIngredients(itemId, ingredientIds(req),
                    issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã ghi hết nguyên liệu vào sổ kho — các món dùng nguyên liệu này "
                    + "tự ẩn khỏi POS/QR, tự hiện lại khi có tồn.");
        // EN: Other blocking reason (broken machine etc). Item leaves the queue, status becomes BLOCKED. -> KdsService.blockItem()
        } else if (reason != null && reason.isBlocking()) {
            if (!service.blockItem(itemId, issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã chuyển món sang mục Cần xử lý.");
        // EN: Non-blocking reason. Just flags a warning for cashier/manager, item keeps brewing. -> KdsService.reportIssue()
        } else {
            if (!service.reportIssue(itemId, issueReason(req), userId, branchId)) flashConflict(req);
            else flashOk(req, "Đã báo sự cố cho Thu ngân/Quản lý. Món chưa bị hủy.");
        }
    }

    // EN: Simple path if no recount param; otherwise parses ingredientId[]/actualQty[] for a stock recount. -> KdsService.unblockItem()
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

    private void loadBoard(HttpServletRequest req, int branchId) throws Exception {
        User current = SessionUtil.currentUser(req);
        Integer currentUserId = current == null ? null : current.getUserId();
        KdsService.KdsBoardQuery query = new KdsService.KdsBoardQuery(
                req.getParameter("owner"), req.getParameter("station"), req.getParameter("orderType"),
                pageParam(req), currentUserId);
        req.setAttribute("board", service.loadBoard(branchId, query));
    }

    private static int pageParam(HttpServletRequest req) {
        return RequestParams.positiveInt(req, "page", 1);
    }

    private static void flashConflict(HttpServletRequest req) {
        req.getSession().setAttribute("flashError", "Món vừa được cập nhật bởi thao tác khác — bảng đã làm mới.");
    }

    private static List<Integer> ingredientIds(HttpServletRequest req) {
        String[] raw = req.getParameterValues("ingredientId");
        List<Integer> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            try { out.add(Integer.valueOf(s.trim())); }
            catch (NumberFormatException ignored) { }
        }
        return out;
    }

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
