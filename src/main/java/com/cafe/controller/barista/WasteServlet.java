package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.User;
import com.cafe.model.WasteLog;
import com.cafe.model.WasteLogLine;
import com.cafe.service.barista.WasteService;
import com.cafe.service.shared.InventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** B5 · WasteServlet → /barista/waste. Ghi hao hụt/làm lại (qua ledger). */
@WebServlet("/barista/waste")
public class WasteServlet extends HttpServlet {

    private final WasteService service = new WasteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        int userId = currentUserId(req);
        try {
            applyExpiredPrefill(req);
            forwardPage(req, resp, branchId, userId, req.getParameter("edit"));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        int userId = currentUserId(req);
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/waste")) return;   // vào ca / chặn ngoài ca
        String editId = null;

        try {
            if ("createIngredientWaste".equals(action) || "create".equals(action)) {
                List<WasteRowForm> submitted = "create".equals(action) ? legacyRow(req) : submittedWasteRows(req);
                req.setAttribute("submittedWasteRows", submitted);
                int count = service.logIngredientWasteLines(branchId, toWasteLines(submitted), userId);
                req.getSession().setAttribute("flashOk", "Đã ghi " + count + " dòng hao hụt.");
            } else if ("remakeProduct".equals(action)) {
                RemakeForm form = submittedRemake(req);
                req.setAttribute("submittedRemake", form);
                int count = service.remakeProduct(branchId, parseInt(form.productId, "Chưa chọn món làm lại."),
                        parseInt(form.quantity, "Số lượng món làm lại không hợp lệ."),
                        form.size, form.iceLevel, form.sugarLevel,
                        combineReason(form.reasonPreset, form.reasonDetail), userId);
                req.getSession().setAttribute("flashOk", "Đã ghi làm lại món (" + count + " dòng nguyên liệu).");
            } else if ("update".equals(action)) {
                editId = req.getParameter("wasteLogId");
                req.setAttribute("editQuantity", req.getParameter("quantity"));
                req.setAttribute("editWasteType", req.getParameter("wasteType"));
                req.setAttribute("editReason", req.getParameter("reason"));
                int wasteLogId = parseInt(editId, "Bản ghi cần sửa không hợp lệ.");
                BigDecimal qty = parseQty(req.getParameter("quantity"), "Số lượng phải > 0.");
                service.updateWaste(branchId, wasteLogId, qty, req.getParameter("wasteType"), req.getParameter("reason"), userId);
                req.getSession().setAttribute("flashOk", "Đã sửa — chênh lệch ghi vào sổ cái.");
            } else if ("void".equals(action)) {
                int wasteLogId = parseInt(req.getParameter("wasteLogId"), "Bản ghi cần huỷ không hợp lệ.");
                service.voidWaste(branchId, wasteLogId, userId);
                req.getSession().setAttribute("flashOk", "Đã huỷ — tồn kho hoàn lại qua sổ cái (txn bù).");
            } else {
                throw new BusinessException("Thao tác không hợp lệ.");
            }
            resp.sendRedirect(req.getContextPath() + "/barista/waste");
        } catch (BusinessException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (NumberFormatException e) {
            req.setAttribute("flashError", "Dữ liệu số không hợp lệ.");
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (IllegalArgumentException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void forwardAfterError(HttpServletRequest req, HttpServletResponse resp, int branchId, int userId, String editId)
            throws ServletException, IOException {
        try {
            forwardPage(req, resp, branchId, userId, editId);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void forwardPage(HttpServletRequest req, HttpServletResponse resp, int branchId, int userId, String editId)
            throws SQLException, ServletException, IOException {
        WasteService.WasteScope scope = service.resolveScope(userId, branchId);
        String logQuery = textParam(req, "q", 100);
        String logWasteType = allowedParam(req, "wasteType", "SPILL", "EXPIRED", "REMAKE", "OTHER");
        String logStatus = allowedParam(req, "status", "ACTIVE", "VOIDED");
        int logPageSize = pageSize();
        int requestedLogPage = positiveIntParam(req, "page", 1);

        // Tổng quan giữ nguyên toàn bộ phạm vi; bảng nhật ký thì chỉ lấy đúng trang từ DB.
        List<WasteLog> scopedLogs = service.getWasteLogs(branchId, scope);
        InventoryService.WasteLogPage wasteLogPage = service.getWasteLogPage(branchId, scope,
                logQuery, logWasteType, logStatus, requestedLogPage, logPageSize);
        req.setAttribute("ingredients", service.getIngredients());
        List<com.cafe.model.BranchMenuItem> remakeProducts = service.getRemakeProducts(branchId);
        req.setAttribute("products", remakeProducts);
        req.setAttribute("scope", scope);
        req.setAttribute("logs", wasteLogPage.getLogs());
        req.setAttribute("hasWasteLogs", !scopedLogs.isEmpty());
        req.setAttribute("wasteLogPage", wasteLogPage);
        req.setAttribute("wasteLogQuery", logQuery);
        req.setAttribute("wasteLogWasteType", logWasteType);
        req.setAttribute("wasteLogStatus", logStatus);
        req.setAttribute("summary", service.summarize(scopedLogs));
        req.setAttribute("pageTitle", "Hao hụt & Làm lại");
        BaristaShift.expose(req, "/barista/waste");   // trực ca: banner + khoá thao tác

        if (req.getAttribute("submittedWasteRows") == null) {
            req.setAttribute("submittedWasteRows", List.of(new WasteRowForm("", "", "SPILL", "", "")));
        }
        if (req.getAttribute("submittedRemake") == null) {
            req.setAttribute("submittedRemake", new RemakeForm("", "1", "M", "Bình thường", "100%", "", ""));
        }
        if (editId != null && !editId.isBlank()) {
            try {
                WasteLog editLog = service.getEditableWasteLog(branchId, Integer.parseInt(editId));
                if (editLog == null) req.setAttribute("flashError", "Bản ghi cần sửa không tồn tại.");
                else req.setAttribute("editLog", editLog);
            } catch (BusinessException e) {
                req.setAttribute("flashError", e.getMessage());
            } catch (NumberFormatException e) {
                req.setAttribute("flashError", "Bản ghi cần sửa không hợp lệ.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/barista/waste.jsp").forward(req, resp);
    }

    private int currentUserId(HttpServletRequest req) {
        User u = SessionUtil.currentUser(req);
        return u != null ? u.getUserId() : 0;
    }

    private List<WasteRowForm> submittedWasteRows(HttpServletRequest req) {
        String[] ids = req.getParameterValues("ingredientId");
        String[] qtys = req.getParameterValues("quantity");
        String[] types = req.getParameterValues("wasteType");
        String[] presets = req.getParameterValues("reasonPreset");
        String[] details = req.getParameterValues("reasonDetail");
        int len = maxLen(ids, qtys, types, presets, details);
        List<WasteRowForm> rows = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            rows.add(new WasteRowForm(value(ids, i), value(qtys, i), value(types, i), value(presets, i), value(details, i)));
        }
        return rows.isEmpty() ? List.of(new WasteRowForm("", "", "SPILL", "", "")) : rows;
    }

    private List<WasteRowForm> legacyRow(HttpServletRequest req) {
        return List.of(new WasteRowForm(req.getParameter("ingredientId"), req.getParameter("quantity"),
                req.getParameter("wasteType"), "", req.getParameter("reason")));
    }

    private void applyExpiredPrefill(HttpServletRequest req) {
        String ingredientId = req.getParameter("ingredientId");
        String qty = req.getParameter("qty");
        if (blank(ingredientId) || blank(qty)) return;
        try {
            int parsedIngredientId = Integer.parseInt(ingredientId.trim());
            BigDecimal parsedQty = new BigDecimal(qty.trim());
            if (parsedIngredientId <= 0 || parsedQty.signum() <= 0) return;
            req.setAttribute("submittedWasteRows", List.of(new WasteRowForm(
                    String.valueOf(parsedIngredientId), parsedQty.stripTrailingZeros().toPlainString(),
                    "EXPIRED", "Hết hạn", "")));
        } catch (NumberFormatException ignored) {
            // Prefill URL params are editable by the user; bad values simply fall back to a blank form.
        }
    }

    private List<WasteLogLine> toWasteLines(List<WasteRowForm> forms) {
        List<WasteLogLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (WasteRowForm form : forms) {
            boolean started = !blank(form.ingredientId) || !blank(form.quantity)
                    || !blank(form.reasonPreset) || !blank(form.reasonDetail);
            if (!started) { lineNo++; continue; }
            if (blank(form.ingredientId)) throw new BusinessException("Dòng " + lineNo + ": Chưa chọn nguyên liệu.");
            int ingredientId = parseInt(form.ingredientId, "Dòng " + lineNo + ": Nguyên liệu không hợp lệ.");
            BigDecimal qty = parseQty(form.quantity, "Dòng " + lineNo + ": Số lượng phải > 0.");
            String type = blank(form.wasteType) ? "OTHER" : form.wasteType.trim();
            lines.add(new WasteLogLine(ingredientId, qty, type, combineReason(form.reasonPreset, form.reasonDetail)));
            lineNo++;
        }
        if (lines.isEmpty()) throw new BusinessException("Chưa có dòng hao hụt nào để ghi.");
        return lines;
    }

    private RemakeForm submittedRemake(HttpServletRequest req) {
        return new RemakeForm(req.getParameter("productId"), req.getParameter("productQty"),
                req.getParameter("productSize"), req.getParameter("iceLevel"), req.getParameter("sugarLevel"),
                req.getParameter("remakeReasonPreset"), req.getParameter("remakeReasonDetail"));
    }

    private static int parseInt(String value, String message) {
        if (blank(value)) throw new BusinessException(message);
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { throw new BusinessException(message); }
    }

    private static BigDecimal parseQty(String value, String message) {
        if (blank(value)) throw new BusinessException(message);
        try {
            BigDecimal qty = new BigDecimal(value.trim());
            if (qty.signum() <= 0) throw new BusinessException(message);
            return qty;
        } catch (NumberFormatException e) {
            throw new BusinessException(message);
        }
    }

    private static String combineReason(String preset, String detail) {
        String p = blank(preset) ? "" : preset.trim();
        String d = blank(detail) ? "" : detail.trim();
        if (p.isEmpty()) return d;
        if (d.isEmpty()) return p;
        String value = p + " - " + d;
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private static String value(String[] arr, int idx) {
        return arr != null && idx < arr.length && arr[idx] != null ? arr[idx] : "";
    }

    private static int maxLen(String[]... arrays) {
        int max = 0;
        for (String[] arr : arrays) if (arr != null && arr.length > max) max = arr.length;
        return max;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String textParam(HttpServletRequest req, String name, int maxLength) {
        String value = req.getParameter(name);
        if (blank(value)) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String allowedParam(HttpServletRequest req, String name, String... allowed) {
        String value = textParam(req, name, 20).toUpperCase();
        for (String item : allowed) if (item.equals(value)) return value;
        return "";
    }

    private static int positiveIntParam(HttpServletRequest req, String name, int fallback) {
        try {
            int value = Integer.parseInt(req.getParameter(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Nhật ký luôn hiển thị 5 dòng/trang để dễ theo dõi tại quầy. */
    private static int pageSize() {
        return 5;
    }

    public static class WasteRowForm {
        private final String ingredientId;
        private final String quantity;
        private final String wasteType;
        private final String reasonPreset;
        private final String reasonDetail;

        public WasteRowForm(String ingredientId, String quantity, String wasteType, String reasonPreset, String reasonDetail) {
            this.ingredientId = ingredientId == null ? "" : ingredientId;
            this.quantity = quantity == null ? "" : quantity;
            this.wasteType = wasteType == null || wasteType.isBlank() ? "SPILL" : wasteType;
            this.reasonPreset = reasonPreset == null ? "" : reasonPreset;
            this.reasonDetail = reasonDetail == null ? "" : reasonDetail;
        }

        public String getIngredientId() { return ingredientId; }
        public String getQuantity() { return quantity; }
        public String getWasteType() { return wasteType; }
        public String getReasonPreset() { return reasonPreset; }
        public String getReasonDetail() { return reasonDetail; }
    }

    public static class RemakeForm {
        private final String productId;
        private final String quantity;
        private final String size;
        private final String iceLevel;
        private final String sugarLevel;
        private final String reasonPreset;
        private final String reasonDetail;

        public RemakeForm(String productId, String quantity, String size, String iceLevel, String sugarLevel,
                          String reasonPreset, String reasonDetail) {
            this.productId = productId == null ? "" : productId;
            this.quantity = quantity == null || quantity.isBlank() ? "1" : quantity;
            this.size = normalizeSize(size);
            this.iceLevel = normalizeIce(iceLevel);
            this.sugarLevel = normalizeSugar(sugarLevel);
            this.reasonPreset = reasonPreset == null ? "" : reasonPreset;
            this.reasonDetail = reasonDetail == null ? "" : reasonDetail;
        }

        public String getProductId() { return productId; }
        public String getQuantity() { return quantity; }
        public String getSize() { return size; }
        public String getIceLevel() { return iceLevel; }
        public String getSugarLevel() { return sugarLevel; }
        public String getReasonPreset() { return reasonPreset; }
        public String getReasonDetail() { return reasonDetail; }

        private static String normalizeSize(String value) {
            if ("S".equalsIgnoreCase(value) || "L".equalsIgnoreCase(value)) return value.toUpperCase(java.util.Locale.ROOT);
            return "M";
        }

        private static String normalizeIce(String value) {
            if ("Không đá".equals(value) || "Ít đá".equals(value) || "Nhiều đá".equals(value)) return value;
            return "Bình thường";
        }

        private static String normalizeSugar(String value) {
            if ("0%".equals(value) || "30%".equals(value) || "50%".equals(value)
                    || "70%".equals(value) || "100%".equals(value)) return value;
            return "100%";
        }
    }
}
