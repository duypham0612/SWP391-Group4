package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            if (sid != null && !sid.isBlank()) req.setAttribute("sessionId", Integer.valueOf(sid));
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
        resp.setContentType("application/json;charset=UTF-8");
        try {
            JsonNode body = mapper.readTree(req.getInputStream());
            Integer sessionId = body.hasNonNull("sessionId") ? body.get("sessionId").asInt() : null;
            String orderType = body.hasNonNull("orderType") ? body.get("orderType").asText() : "DINE_IN";

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
}
