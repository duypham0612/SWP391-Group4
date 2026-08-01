package com.cafe.controller.cashier;

import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.model.User;
import com.cafe.model.PosMenuItem;
import com.cafe.service.cashier.PickupService;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.service.shared.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * C4 · OrderInboxServlet → /cashier/inbox.
 * GIÁM SÁT đơn đang xử lý (COUNTER + QR, cùng OrderService) + VOID đơn sai.
 * KHÔNG chặn luồng: đơn vẫn auto vào KDS như cũ; inbox chỉ theo dõi & huỷ đơn sai.
 */
@WebServlet("/cashier/inbox")
public class OrderInboxServlet extends HttpServlet {

    private final OrderService orderService;
    private final PickupService pickupService;
    private final CatalogReadService catalogReadService;

    public OrderInboxServlet() { this(new OrderService(), new PickupService(), new CatalogReadService()); }
    OrderInboxServlet(OrderService orderService, PickupService pickupService,
                      CatalogReadService catalogReadService) {
        this.orderService = java.util.Objects.requireNonNull(orderService);
        this.pickupService = java.util.Objects.requireNonNull(pickupService);
        this.catalogReadService = java.util.Objects.requireNonNull(catalogReadService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        try {
            java.util.List<com.cafe.model.Order> orders = orderService.getIncomingOrders(branchId);
            // Đơn treo đã được service xếp lên đầu; đếm ở đây để có một dòng nhắc gọn trên cùng —
            // quầy pha chế không còn nhận những đơn này nên đây là chỗ duy nhất chốt được chúng.
            int staleCount = 0;
            for (com.cafe.model.Order o : orders) if (o.isStale()) staleCount++;
            req.setAttribute("orders", orders);
            req.setAttribute("staleOrderCount", staleCount);
            java.util.List<PosMenuItem> menu = catalogReadService.getPosMenu(branchId);
            req.setAttribute("lowStockItems", menu.stream()
                    .filter(item -> "LOW".equals(item.getAvailabilityState())).toList());
            req.setAttribute("outOfStockItems", menu.stream()
                    .filter(item -> !item.isOrderable()).toList());
            loadHandoff(req, branchId);
            req.setAttribute("pageTitle", "Đơn đến & Bàn giao");
            req.getRequestDispatcher("/WEB-INF/views/cashier/inbox.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        String action = req.getParameter("action");
        try {
            if ("void".equals(action)) {
                int orderId = Integer.parseInt(req.getParameter("orderId"));
                boolean ok = orderService.voidOrder(orderId, userId, branchId);
                req.getSession().setAttribute(ok ? "flashOk" : "flashError",
                        ok ? "Đã huỷ đơn — các món chưa pha chuyển CANCELLED (không đụng tồn)."
                           : "Không thể huỷ — đơn đã được pha (hoặc đã xử lý).");
            } else if ("cancelItem".equals(action)) {
                // Huỷ một dòng món (đặc biệt món BLOCKED: hết nguyên liệu/hỏng máy → thoát bế tắc).
                int orderItemId = Integer.parseInt(req.getParameter("orderItemId"));
                String reason = req.getParameter("reason");
                String code = orderService.cancelItem(orderItemId, reason, userId, branchId);
                req.getSession().setAttribute("OK".equals(code) ? "flashOk" : "flashError",
                        cancelItemMessage(code));
            } else if ("pickUp".equals(action)) {
                if (!pickupService.pickUpItem(intParam(req, "orderItemId"), userId, branchId)) {
                    flashHandoffConflict(req);
                }
            } else if ("pickUpAllReady".equals(action)) {
                int done = pickupService.pickUpAllReady(intParam(req, "orderId"), userId, branchId);
                if (done == 0) flashHandoffConflict(req);
                else req.getSession().setAttribute("flashOk",
                        "Đã nhận " + done + " dòng món khỏi quầy.");
            } else if ("serve".equals(action)) {
                if (!pickupService.serveItem(intParam(req, "orderItemId"), userId, branchId)) {
                    flashHandoffConflict(req);
                }
            } else if ("serveAll".equals(action)) {
                int done = pickupService.serveAllPickedUp(
                        positiveDistinctInts(req.getParameterValues("orderId")),
                        req.getParameter("tableNumber"), userId, branchId);
                if (done == 0) flashHandoffConflict(req);
                else req.getSession().setAttribute("flashOk",
                        "Đã giao tất cả " + done + " dòng món của bàn.");
            }
            String anchor = action != null && (action.startsWith("pickUp") || action.startsWith("serve"))
                    ? "#handoff" : "#orders";
            resp.sendRedirect(req.getContextPath() + "/cashier/inbox" + anchor);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/cashier/inbox");
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Thông điệp theo mã kết quả cancelItem (OK/NOT_FOUND/ALREADY_BILLED/CONFLICT). */
    private static String cancelItemMessage(String code) {
        switch (code == null ? "" : code) {
            case "OK": return "Đã huỷ món.";
            case "ALREADY_BILLED": return "Không huỷ được — món đã lên hoá đơn, xử lý ở Thanh toán.";
            case "NOT_FOUND": return "Không tìm thấy món.";
            default: return "Không huỷ được — món đã được pha hoặc vừa thay đổi.";
        }
    }

    private void loadHandoff(HttpServletRequest req, int branchId) throws Exception {
        req.setAttribute("tickets", pickupService.getReadyTickets(branchId));
        req.setAttribute("pickedUpGroups", pickupService.getPickedUpGroups(branchId));
        req.setAttribute("embeddedHandoff", true);
    }

    private static void flashHandoffConflict(HttpServletRequest req) {
        req.getSession().setAttribute("flashError",
                "Món vừa được cập nhật bởi thao tác khác — danh sách đã được làm mới.");
    }

    private static int intParam(HttpServletRequest req, String name) {
        return Integer.parseInt(req.getParameter(name));
    }

    private static java.util.List<Integer> positiveDistinctInts(String[] values) {
        if (values == null) return java.util.List.of();
        return java.util.Arrays.stream(values)
                .map(value -> {
                    try { return Integer.valueOf(value); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .filter(value -> value > 0)
                .distinct()
                .limit(50)
                .toList();
    }
}
