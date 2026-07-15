package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.Ingredient;
import com.cafe.model.User;
import com.cafe.service.barista.PrepService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** B4 · PrepServlet → /barista/prep. Pha sẵn (RAW→PREPPED, Contract #2) + checklist + guard tồn. */
@WebServlet("/barista/prep")
public class PrepServlet extends HttpServlet {

    private final PrepService service = new PrepService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("1".equals(req.getParameter("stock"))) {        // làm mới tồn RAW (AJAX, không reload form)
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write(service.getRawOnHandJson(branchId));
                return;
            }
            forwardPage(req, resp, branchId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/prep")) return;   // vào ca / chặn ngoài ca
        List<SubmittedPrepRow> submittedRows = "createBatch".equals(action) ? submittedRows(req) : List.of();
        try {
            if ("createBatch".equals(action)) {
                int created = handleCreate(submittedRows, branchId, userId);
                req.getSession().setAttribute("flashOk", "Đã tạo " + created + " mẻ pha — tồn cập nhật qua sổ cái.");
                resp.sendRedirect(req.getContextPath() + "/barista/prep");
                return;
            } else if ("cancelBatch".equals(action)) {
                service.cancelBatch(branchId, Integer.parseInt(req.getParameter("prepBatchId")), userId);
                req.getSession().setAttribute("flashOk", "Đã huỷ mẻ — tồn kho hoàn lại qua sổ cái (txn bù).");
            } else if ("updateBatch".equals(action)) {
                int batchId = Integer.parseInt(req.getParameter("prepBatchId"));
                BigDecimal qty = new BigDecimal(req.getParameter("quantityProduced").trim());
                if (qty.signum() > 0) {
                    service.updateBatch(branchId, batchId, qty, userId);
                    req.getSession().setAttribute("flashOk", "Đã cập nhật sản lượng — chênh lệch ghi vào sổ cái.");
                } else {
                    req.getSession().setAttribute("flashError", "Sản lượng phải > 0.");
                }
            }
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (BusinessException e) {                         // vi phạm nghiệp vụ → flash, không 500
            if ("createBatch".equals(action)) {
                forwardCreateError(req, resp, branchId, submittedRows, e.getMessage());
                return;
            }
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (NumberFormatException e) {
            if ("createBatch".equals(action)) {
                forwardCreateError(req, resp, branchId, submittedRows, "Sản lượng không hợp lệ.");
            } else {
                req.getSession().setAttribute("flashError", "Sản lượng không hợp lệ.");
                resp.sendRedirect(req.getContextPath() + "/barista/prep");
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Gom nhiều dòng (mảng) → tạo nhiều mẻ một lần (tất cả-hoặc-không). */
    private int handleCreate(List<SubmittedPrepRow> submitted, int branchId, int userId) throws SQLException {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        Map<Integer, Ingredient> preppedById = preppedById();
        List<com.cafe.model.PrepBatchLine> lines = new ArrayList<>();
        for (SubmittedPrepRow row : submitted) {
            if (!row.started()) continue;
            String lineLabel = "Dòng " + row.lineNo;
            if (blank(row.preppedIngredientId)) throw new BusinessException(lineLabel + ": Chưa chọn nguyên liệu pha sẵn.");
            int ingredientId;
            try {
                ingredientId = Integer.parseInt(row.preppedIngredientId.trim());
            } catch (NumberFormatException e) {
                throw new BusinessException(lineLabel + ": Nguyên liệu pha sẵn không hợp lệ.");
            }
            Ingredient ingredient = preppedById.get(ingredientId);
            if (ingredient == null) throw new BusinessException(lineLabel + ": Nguyên liệu pha sẵn không hợp lệ.");
            String name = ingredient.getName();
            if (blank(row.quantityProduced)) throw new BusinessException(name + ": Chưa nhập sản lượng.");
            BigDecimal qty;
            try {
                qty = new BigDecimal(row.quantityProduced.trim());
            } catch (NumberFormatException e) {
                throw new BusinessException(name + ": Sản lượng không hợp lệ.");
            }
            if (qty.signum() <= 0) throw new BusinessException(name + ": Sản lượng phải > 0.");
            LocalDateTime exp = parseExpiry(row.expiresAt, name);
            if (exp != null && !exp.isAfter(nowUtc)) throw new BusinessException(name + ": Hạn dùng phải ở tương lai.");
            lines.add(new com.cafe.model.PrepBatchLine(ingredientId, qty, exp, name));
        }
        if (lines.isEmpty()) throw new BusinessException("Chưa chọn nguyên liệu nào để pha.");

        service.createBatches(branchId, lines, userId);
        return lines.size();
    }

    /** input datetime-local (giờ VN) → LocalDateTime UTC để lưu DB. */
    private LocalDateTime parseExpiry(String raw, String name) {
        if (raw == null || raw.isBlank()) return null;
        try {
            LocalDateTime local = LocalDateTime.parse(raw.trim());   // yyyy-MM-ddTHH:mm
            return local.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            throw new BusinessException(name + ": Hạn dùng không hợp lệ.");
        }
    }

    private void forwardCreateError(HttpServletRequest req, HttpServletResponse resp, int branchId,
                                    List<SubmittedPrepRow> submittedRows, String message)
            throws ServletException, IOException {
        req.setAttribute("flashError", message);
        req.setAttribute("submittedPrepRowsJson", submittedRowsJson(submittedRows));
        try {
            forwardPage(req, resp, branchId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void forwardPage(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws ServletException, IOException, SQLException {
        List<Ingredient> prepped = service.getPreppedIngredients();
        req.setAttribute("preppedIngredients", prepped);
        req.setAttribute("checklist", service.getPrepChecklist(branchId));
        req.setAttribute("batches", service.getTodayBatches(branchId));
        req.setAttribute("recipeJson", service.getRecipeJson(prepped));
        req.setAttribute("rawOnHandJson", service.getRawOnHandJson(branchId));
        req.setAttribute("pageTitle", "Pha sẵn nguyên liệu");
        BaristaShift.expose(req, "/barista/prep");   // trực ca: banner + khoá thao tác
        req.getRequestDispatcher("/WEB-INF/views/barista/prep.jsp").forward(req, resp);
    }

    private Map<Integer, Ingredient> preppedById() throws SQLException {
        Map<Integer, Ingredient> out = new LinkedHashMap<>();
        for (Ingredient i : service.getPreppedIngredients()) out.put(i.getIngredientId(), i);
        return out;
    }

    private List<SubmittedPrepRow> submittedRows(HttpServletRequest req) {
        String[] ids = req.getParameterValues("preppedIngredientId");
        String[] qtys = req.getParameterValues("quantityProduced");
        String[] exps = req.getParameterValues("expiresAt");
        int count = Math.max(len(ids), Math.max(len(qtys), len(exps)));
        List<SubmittedPrepRow> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new SubmittedPrepRow(i + 1, value(ids, i), value(qtys, i), value(exps, i)));
        }
        return rows;
    }

    private String submittedRowsJson(List<SubmittedPrepRow> rows) {
        if (rows == null || rows.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            SubmittedPrepRow r = rows.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"preppedIngredientId\":\"").append(json(r.preppedIngredientId))
              .append("\",\"quantityProduced\":\"").append(json(r.quantityProduced))
              .append("\",\"expiresAt\":\"").append(json(r.expiresAt)).append("\"}");
        }
        return sb.append(']').toString();
    }

    private static int len(String[] a) { return a == null ? 0 : a.length; }
    private static String value(String[] a, int i) { return a != null && i < a.length && a[i] != null ? a[i] : ""; }
    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("<", "\\u003C").replace(">", "\\u003E")
                .replace("&", "\\u0026").replace("'", "\\u0027")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static class SubmittedPrepRow {
        final int lineNo;
        final String preppedIngredientId;
        final String quantityProduced;
        final String expiresAt;

        SubmittedPrepRow(int lineNo, String preppedIngredientId, String quantityProduced, String expiresAt) {
            this.lineNo = lineNo;
            this.preppedIngredientId = preppedIngredientId;
            this.quantityProduced = quantityProduced;
            this.expiresAt = expiresAt;
        }

        boolean started() {
            return !blank(preppedIngredientId) || !blank(quantityProduced) || !blank(expiresAt);
        }
    }
}
