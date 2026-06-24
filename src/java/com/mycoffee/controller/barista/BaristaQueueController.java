package com.mycoffee.controller.barista;

import com.mycoffee.dao.BaristaDAO;
import com.mycoffee.model.BaristaQueueItem;
import com.mycoffee.model.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Màn 2: Hàng chờ pha chế (Kanban realtime).
 * Kèm endpoint JSON (?action=queue) cho polling và các thao tác đổi trạng thái (doPost).
 */
@WebServlet(name = "BaristaQueueController", urlPatterns = {"/barista-board"})
public class BaristaQueueController extends HttpServlet {

    private final BaristaDAO dao = new BaristaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() > 4) {
            response.sendRedirect("login");
            return;
        }
        Integer b = (Integer) session.getAttribute("branchId");
        int branchId = (b == null) ? 1 : b;

        if ("queue".equals(request.getParameter("action"))) {
            writeQueueJson(response, dao.getQueue(branchId)); // JSON cho realtime
            return;
        }
        request.setAttribute("queueList", dao.getQueue(branchId));
        request.getRequestDispatcher("/views/barista/barista_queue.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() > 4) {
            writeJson(response, "{\"ok\":false,\"error\":\"unauthorized\"}");
            return;
        }

        String action = request.getParameter("action");
        boolean ok = false;
        if ("updateStatus".equals(action)) {
            ok = dao.updateItemStatus(parseInt(request.getParameter("orderDetailId")), request.getParameter("status"));
        } else if ("outOfStock".equals(action)) {
            ok = dao.markProductOutOfStock(parseInt(request.getParameter("productId")));
        } else if ("bumpPriority".equals(action)) {
            ok = dao.togglePriority(parseInt(request.getParameter("orderId")));
        }
        writeJson(response, "{\"ok\":" + ok + "}");
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(json);
        }
    }

    private void writeQueueJson(HttpServletResponse response, List<BaristaQueueItem> list) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            BaristaQueueItem it = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"orderDetailId\":").append(it.getOrderDetailId()).append(",")
              .append("\"orderId\":").append(it.getOrderId()).append(",")
              .append("\"productId\":").append(it.getProductId()).append(",")
              .append("\"tableName\":\"").append(esc(it.getTableName())).append("\",")
              .append("\"productName\":\"").append(esc(it.getProductName())).append("\",")
              .append("\"quantity\":").append(it.getQuantity()).append(",")
              .append("\"note\":\"").append(esc(it.getNote())).append("\",")
              .append("\"itemStatus\":\"").append(esc(it.getItemStatus())).append("\",")
              .append("\"priority\":").append(it.getPriority()).append(",")
              .append("\"orderEpoch\":").append(it.getOrderDate() != null ? it.getOrderDate().getTime() : 0).append(",")
              .append("\"startedEpoch\":").append(it.getStartedAt() != null ? it.getStartedAt().getTime() : 0)
              .append("}");
        }
        sb.append("]");
        writeJson(response, sb.toString());
    }

    private String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"':  b.append("\\\""); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.toString();
    }
}
