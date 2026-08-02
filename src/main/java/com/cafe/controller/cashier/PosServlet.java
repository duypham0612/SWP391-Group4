package com.cafe.controller.cashier;

import com.cafe.common.Constants;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.common.ItemUnavailableException;
import com.cafe.model.PosMenuItem;
import com.cafe.model.DiningTable;
import com.cafe.model.User;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.model.CartLine;
import com.cafe.service.shared.OrderService;
import com.cafe.service.cashier.DiningTableService;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.OrderCartForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** C2 · PosServlet → /cashier/pos. Đặt đơn tại quầy (giỏ JS → submit JSON). Contract #1. */
@WebServlet("/cashier/pos")
public class PosServlet extends HttpServlet {

    private final CatalogReadService catalogReadService;
    private final DiningTableService tableService;
    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    public PosServlet() { this(new CatalogReadService(), new DiningTableService(), new OrderService()); }
    PosServlet(CatalogReadService catalogReadService, DiningTableService tableService,
               OrderService orderService) {
        this.catalogReadService = java.util.Objects.requireNonNull(catalogReadService);
        this.tableService = java.util.Objects.requireNonNull(tableService);
        this.orderService = java.util.Objects.requireNonNull(orderService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        try {
            List<PosMenuItem> menu = catalogReadService.getPosMenu(branchId);
            req.setAttribute("menu", menu);
            req.setAttribute("lowStockItems", menu.stream()
                    .filter(item -> "LOW".equals(item.getAvailabilityState())).toList());
            req.setAttribute("outOfStockItems", menu.stream()
                    .filter(item -> !item.isOrderable()).toList());
            req.setAttribute("openTables", tableService.getOpenTables(branchId));
            String tid = req.getParameter("tableId");
            if (tid != null && !tid.isBlank()) {
                int tableId = Integer.parseInt(tid);
                if (!belongsToBranch(tableId, branchId)) {
                    req.getSession().setAttribute("flashError", "Bàn không hợp lệ.");
                    resp.sendRedirect(req.getContextPath() + "/cashier/table");
                    return;
                }
                req.setAttribute("tableId", tableId);
                req.setAttribute("draftCartJson", getDraftCart(req, tableId));
                req.setAttribute("tableItems", orderService.getTableItemStatuses(tableId));
            }
            req.setAttribute("pageTitle", "POS — Đặt món");
            req.getRequestDispatcher("/WEB-INF/views/cashier/pos.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        try {
            if ("saveDraft".equals(action)) {
                Integer tableId = parseNullableInt(req.getParameter("tableId"));
                if (tableId != null && belongsToBranch(tableId, branchId)) {
                    saveDraftCart(req, tableId, req.getParameter("cartJson"));
                }
                resp.sendRedirect(req.getContextPath() + "/cashier/table");
                return;
            }
            if ("discardDraft".equals(action)) {
                Integer tableId = parseNullableInt(req.getParameter("tableId"));
                if (tableId != null && belongsToBranch(tableId, branchId)) {
                    removeDraftCart(req.getSession(), tableId);
                    tableService.closeTableIfNoUnpaidOrders(tableId, branchId);
                }
                resp.sendRedirect(req.getContextPath() + "/cashier/table");
                return;
            }

            resp.setContentType("application/json;charset=UTF-8");
            OrderCartForm form = OrderCartForm.fromJson(req, mapper);
            // OrderCartForm giữ tên field wire cũ để tương thích client; giá trị hiện là tableId.
        Integer tableId = form.tableId();
            if (tableId != null && !belongsToBranch(tableId, branchId)) {
                throw new IllegalArgumentException("Bàn không hợp lệ.");
            }
            String orderType = tableId == null ? "TAKEAWAY" : "DINE_IN";
            List<CartLine> lines = form.toCartLines();

            int orderId = orderService.placeOrder(branchId, tableId, "COUNTER", orderType, userId, lines);
            if (tableId != null) removeDraftCart(req.getSession(), tableId);
            resp.getWriter().write("{\"orderId\":" + orderId + "}");
        } catch (ItemUnavailableException e) {
            resp.setStatus(409);
            mapper.writeValue(resp.getWriter(), Map.of(
                    "code", "ITEM_UNAVAILABLE",
                    "productId", e.getProductId(),
                    "productName", e.getProductName(),
                    "state", e.getState(),
                    "error", e.getReason()));
        } catch (IllegalArgumentException | FormBindingException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String escape(String s) { return s == null ? "" : s.replace("\"", "'"); }

    private String getDraftCart(HttpServletRequest req, int tableId) {
        Map<Integer, String> drafts = getDrafts(req.getSession(false));
        String json = drafts == null ? null : drafts.get(tableId);
        return json == null || json.isBlank() ? "[]" : json;
    }

    private void saveDraftCart(HttpServletRequest req, int tableId, String cartJson) throws IOException {
        JsonNode node = mapper.readTree(cartJson == null || cartJson.isBlank() ? "[]" : cartJson);
        if (!node.isArray()) throw new IllegalArgumentException("Giỏ nháp không hợp lệ");
        String safeJson = mapper.writeValueAsString(node).replace("</", "<\\/");
        getOrCreateDrafts(req.getSession()).put(tableId, safeJson);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String> getDrafts(HttpSession session) {
        if (session == null) return null;
        Object value = session.getAttribute(Constants.SESSION_DRAFT_CARTS);
        return value instanceof Map<?, ?> ? (Map<Integer, String>) value : null;
    }

    private Map<Integer, String> getOrCreateDrafts(HttpSession session) {
        Map<Integer, String> drafts = getDrafts(session);
        if (drafts == null) {
            drafts = new HashMap<>();
            session.setAttribute(Constants.SESSION_DRAFT_CARTS, drafts);
        }
        return drafts;
    }

    private void removeDraftCart(HttpSession session, int tableId) {
        Map<Integer, String> drafts = getDrafts(session);
        if (drafts != null) drafts.remove(tableId);
    }

    private boolean belongsToBranch(int tableId, int branchId) throws Exception {
        DiningTable table = tableService.getTable(tableId);
        return table != null && table.getBranchId() == branchId && "OCCUPIED".equals(table.getStatus());
    }

    private Integer parseNullableInt(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
