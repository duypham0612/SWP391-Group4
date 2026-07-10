package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.TableSession;
import com.cafe.model.User;
import com.cafe.service.shared.CatalogReadService;
import com.cafe.service.shared.OrderService;
import com.cafe.service.cashier.TableSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** C2 · PosServlet → /cashier/pos. Đặt đơn tại quầy (giỏ JS → submit JSON). Contract #1. */
@WebServlet("/cashier/pos")
public class PosServlet extends HttpServlet {

    private final CatalogReadService catalogReadService = new CatalogReadService();
    private final TableSessionService tableSessionService = new TableSessionService();
    private final OrderService orderService = new OrderService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("menu", catalogReadService.getPosMenu(branchId));
            req.setAttribute("openSessions", tableSessionService.getOpenSessions(branchId));
            String sid = req.getParameter("sessionId");
            if (sid != null && !sid.isBlank()) {
                int sessionId = Integer.parseInt(sid);
                if (!belongsToBranch(sessionId, branchId)) {
                    req.getSession().setAttribute("flashError", "Phiên bàn không hợp lệ.");
                    resp.sendRedirect(req.getContextPath() + "/cashier/table");
                    return;
                }
                req.setAttribute("sessionId", sessionId);
                req.setAttribute("draftCartJson", getDraftCart(req, sessionId));
                req.setAttribute("sessionItems", orderService.getSessionItemStatuses(sessionId));
            }
            req.setAttribute("pageTitle", "POS — Đặt món");
            req.getRequestDispatcher("/WEB-INF/views/cashier/pos.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        try {
            if ("saveDraft".equals(action)) {
                Integer sessionId = parseNullableInt(req.getParameter("sessionId"));
                if (sessionId != null && belongsToBranch(sessionId, branchId)) {
                    saveDraftCart(req, sessionId, req.getParameter("cartJson"));
                }
                resp.sendRedirect(req.getContextPath() + "/cashier/table");
                return;
            }
            if ("discardDraft".equals(action)) {
                Integer sessionId = parseNullableInt(req.getParameter("sessionId"));
                if (sessionId != null && belongsToBranch(sessionId, branchId)) {
                    removeDraftCart(req.getSession(), sessionId);
                    tableSessionService.closeSessionIfNoActiveItems(sessionId);
                }
                resp.sendRedirect(req.getContextPath() + "/cashier/table");
                return;
            }

            resp.setContentType("application/json;charset=UTF-8");
            JsonNode body = mapper.readTree(req.getInputStream());
            Integer sessionId = body.hasNonNull("sessionId") ? body.get("sessionId").asInt() : null;
            String orderType = body.hasNonNull("orderType") ? body.get("orderType").asText() : "DINE_IN";
            if (sessionId != null && !belongsToBranch(sessionId, branchId)) {
                throw new IllegalArgumentException("Phiên bàn không hợp lệ.");
            }

            List<OrderService.CartLine> lines = new ArrayList<>();
            JsonNode items = body.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode n : items) {
                    OrderService.CartLine line = new OrderService.CartLine();
                    line.productId = n.get("productId").asInt();
                    line.quantity = n.has("quantity") ? n.get("quantity").asInt() : 1;
                    line.note = n.hasNonNull("note") ? n.get("note").asText() : null;
                    JsonNode opts = n.get("optionIds");
                    if (opts != null && opts.isArray()) for (JsonNode o : opts) line.optionIds.add(o.asInt());
                    lines.add(line);
                }
            }
            if (lines.isEmpty()) { resp.setStatus(400); resp.getWriter().write("{\"error\":\"Đơn rỗng\"}"); return; }

            int orderId = orderService.placeOrder(branchId, sessionId, "COUNTER", orderType, userId, lines);
            if (sessionId != null) removeDraftCart(req.getSession(), sessionId);
            resp.getWriter().write("{\"orderId\":" + orderId + "}");
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String escape(String s) { return s == null ? "" : s.replace("\"", "'"); }

    private String getDraftCart(HttpServletRequest req, int sessionId) {
        Map<Integer, String> drafts = getDrafts(req.getSession(false));
        String json = drafts == null ? null : drafts.get(sessionId);
        return json == null || json.isBlank() ? "[]" : json;
    }

    private void saveDraftCart(HttpServletRequest req, int sessionId, String cartJson) throws IOException {
        JsonNode node = mapper.readTree(cartJson == null || cartJson.isBlank() ? "[]" : cartJson);
        if (!node.isArray()) throw new IllegalArgumentException("Giỏ nháp không hợp lệ");
        String safeJson = mapper.writeValueAsString(node).replace("</", "<\\/");
        getOrCreateDrafts(req.getSession()).put(sessionId, safeJson);
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

    private void removeDraftCart(HttpSession session, int sessionId) {
        Map<Integer, String> drafts = getDrafts(session);
        if (drafts != null) drafts.remove(sessionId);
    }

    private boolean belongsToBranch(int sessionId, int branchId) throws Exception {
        TableSession session = tableSessionService.getSession(sessionId);
        return session != null && session.getBranchId() == branchId && "OPEN".equals(session.getStatus());
    }

    private Integer parseNullableInt(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
