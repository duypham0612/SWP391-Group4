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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** B5 · WasteServlet → /barista/waste. Ghi hao hụt nguyên liệu (qua ledger); làm lại ghi từ KDS. */
@WebServlet("/barista/waste")
public class WasteServlet extends HttpServlet {

    private final WasteService service = new WasteService();
    private static final int MAX_WASTE_ROWS = 20;
    private static final Map<String, Set<String>> PRESETS_BY_TYPE = Map.of(
            "SPILL", Set.of("Đổ khi pha", "Rơi khi thao tác", "Sai định lượng"),
            "EXPIRED", Set.of("Hết hạn", "Nguyên liệu hỏng", "Bảo quản lỗi", "Quá thời gian mở nắp"),
            "OTHER", Set.of("Mẫu thử/QC", "Khác"));
    private static final Map<String, String> INGREDIENT_CAUSES = Map.of(
            "Đổ khi pha", "SPILL", "Rơi khi thao tác", "SPILL", "Sai định lượng", "WRONG_RECIPE",
            "Hết hạn", "EXPIRED", "Nguyên liệu hỏng", "EXPIRED", "Bảo quản lỗi", "STORAGE",
            "Quá thời gian mở nắp", "STORAGE", "Mẫu thử/QC", "QC_SAMPLE", "Khác", "OTHER");

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
        if (!BaristaWritePolicy.isWasteAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(req.getContextPath() + "/barista/waste");
            return;
        }
        if (BaristaShift.guardWrite(req, resp, action, "/barista/waste")) return;   // vào ca / chặn ngoài ca
        String editId = null;

        try {
            if ("createIngredientWaste".equals(action) || "create".equals(action)) {
                List<WasteRowForm> submitted = "create".equals(action) ? legacyRow(req) : submittedWasteRows(req);
                req.setAttribute("submittedWasteRows", submitted);
                String clientRequestId = requestId(req);
                req.setAttribute("wasteClientRequestId", clientRequestId);
                int count = service.logIngredientWasteLines(branchId, toWasteLines(submitted, !"create".equals(action)), userId, clientRequestId);
                req.getSession().setAttribute("flashOk", count == 0 ? "Yêu cầu này đã được ghi trước đó." : "Đã ghi " + count + " dòng hao hụt.");
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
            // Dòng vừa ghi nằm trên cùng (LoggedAt DESC) nên về trang 1 mới thấy; sửa/huỷ thì giữ nguyên trang.
            boolean created = "createIngredientWaste".equals(action) || "create".equals(action);
            resp.sendRedirect(selfUrlKeepingFilters(req, created ? 1 : null));
        } catch (BusinessException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (NumberFormatException e) {
            req.setAttribute("flashError", "Dữ liệu số không hợp lệ.");
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (IllegalArgumentException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (SQLException e) {
            // Hạ tầng lỗi thì redirect an toàn; không forward lại để tránh truy vấn DB hỏng lần thứ hai.
            req.getSession().setAttribute("flashError", "Không thể cập nhật hao hụt lúc này. Vui lòng thử lại.");
            resp.sendRedirect(selfUrlKeepingFilters(req, null));
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
        String logWasteType = logTypeParam(req);
        String logStatus = allowedParam(req, "status", "ACTIVE", "VOIDED");
        int logPageSize = pageSizeParam(req);
        int requestedLogPage = positiveIntParam(req, "page", 1);

        // Tổng quan giữ nguyên toàn bộ phạm vi; bảng nhật ký thì chỉ lấy đúng trang từ DB.
        List<WasteLog> scopedLogs = service.getWasteLogs(branchId, scope);
        InventoryService.WasteLogPage wasteLogPage = service.getWasteLogPage(branchId, scope,
                logQuery, logWasteType, logStatus, requestedLogPage, logPageSize);
        req.setAttribute("ingredients", service.getIngredients(branchId));
        req.setAttribute("scope", scope);
        req.setAttribute("logs", wasteLogPage.getLogs());
        req.setAttribute("hasWasteLogs", !scopedLogs.isEmpty());
        req.setAttribute("wasteLogPage", wasteLogPage);
        req.setAttribute("wasteLogQuery", logQuery);
        req.setAttribute("wasteLogWasteType", logWasteType);
        req.setAttribute("wasteLogStatus", logStatus);
        req.setAttribute("summary", service.summarize(scopedLogs));
        req.setAttribute("pageTitle", "Hao hụt & Làm lại");
        req.setAttribute("currentUserId", userId);
        BaristaShift.expose(req, "/barista/waste");   // trực ca: banner + khoá thao tác

        if (req.getAttribute("submittedWasteRows") == null) {
            req.setAttribute("submittedWasteRows", List.of(new WasteRowForm("", "", "SPILL", "", "")));
        }
        if (req.getAttribute("wasteClientRequestId") == null) {
            req.setAttribute("wasteClientRequestId", UUID.randomUUID().toString());
        }
        if (editId != null && !editId.isBlank()) {
            try {
                WasteLog editLog = service.getEditableWasteLog(branchId, Integer.parseInt(editId), userId);
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
        if (ids == null || qtys == null || types == null || presets == null || details == null
                || ids.length != qtys.length || ids.length != types.length || ids.length != presets.length || ids.length != details.length) {
            throw new BusinessException("Dữ liệu các dòng hao hụt không đầy đủ. Vui lòng tải lại màn hình và thử lại.");
        }
        int len = ids.length;
        if (len > MAX_WASTE_ROWS) throw new BusinessException("Mỗi lần chỉ được ghi tối đa " + MAX_WASTE_ROWS + " dòng hao hụt.");
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

    private List<WasteLogLine> toWasteLines(List<WasteRowForm> forms, boolean requirePreset) {
        Map<WasteLineKey, WasteLogLine> grouped = new LinkedHashMap<>();
        int lineNo = 1;
        for (WasteRowForm form : forms) {
            boolean started = !blank(form.ingredientId) || !blank(form.quantity)
                    || !blank(form.reasonPreset) || !blank(form.reasonDetail);
            if (!started) { lineNo++; continue; }
            if (blank(form.ingredientId)) throw new BusinessException("Dòng " + lineNo + ": Chưa chọn nguyên liệu.");
            int ingredientId = parseInt(form.ingredientId, "Dòng " + lineNo + ": Nguyên liệu không hợp lệ.");
            BigDecimal qty = parseQty(form.quantity, "Dòng " + lineNo + ": Số lượng phải > 0.");
            String type = blank(form.wasteType) ? "OTHER" : form.wasteType.trim();
            String cause = requireIngredientCause(type, form.reasonPreset, form.reasonDetail, lineNo, requirePreset);
            String reason = combineReason(form.reasonPreset, form.reasonDetail);
            if (blank(reason)) throw new BusinessException("Dòng " + lineNo + ": Vui lòng chọn hoặc nhập lý do.");
            WasteLineKey key = new WasteLineKey(ingredientId, type.toUpperCase(), cause, reason);
            WasteLogLine existing = grouped.get(key);
            if (existing == null) grouped.put(key, new WasteLogLine(ingredientId, qty, type, reason, cause));
            else existing.setQuantity(existing.getQuantity().add(qty));
            lineNo++;
        }
        List<WasteLogLine> lines = new ArrayList<>(grouped.values());
        if (lines.isEmpty()) throw new BusinessException("Chưa có dòng hao hụt nào để ghi.");
        return lines;
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
        return p + " - " + d;
    }

    private static String requestId(HttpServletRequest req) {
        String raw = req.getParameter("clientRequestId");
        if (raw == null) throw new BusinessException("Thiếu mã chống gửi trùng. Vui lòng tải lại màn hình và thử lại.");
        raw = raw.trim();
        if (!raw.matches("[A-Za-z0-9-]{8,60}")) {
            throw new BusinessException("Mã gửi không hợp lệ. Vui lòng tải lại màn hình và thử lại.");
        }
        return raw;
    }

    static String requireIngredientCause(String wasteType, String preset, String detail, int lineNo, boolean requirePreset) {
        String type = wasteType == null ? "" : wasteType.trim().toUpperCase();
        if (!PRESETS_BY_TYPE.containsKey(type)) throw new BusinessException("Dòng " + lineNo + ": Loại hao hụt không hợp lệ.");
        String chosen = preset == null ? "" : preset.trim();
        if (chosen.isEmpty() && !requirePreset) return "OTHER".equals(type) ? "OTHER" : type;
        if (!PRESETS_BY_TYPE.get(type).contains(chosen)) {
            throw new BusinessException("Dòng " + lineNo + ": Lý do không phù hợp với loại hao hụt đã chọn.");
        }
        if ("Khác".equals(chosen) && blank(detail)) {
            throw new BusinessException("Dòng " + lineNo + ": Chọn Khác thì phải nhập diễn giải.");
        }
        return INGREDIENT_CAUSES.get(chosen);
    }

    private static String value(String[] arr, int idx) {
        return arr != null && idx < arr.length && arr[idx] != null ? arr[idx] : "";
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

    /** Nhật ký mặc định 5 dòng/trang cho dễ theo dõi tại quầy; barista chọn được 10/20/50 khi cần soát lại. */
    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(positiveIntParam(req, "pageSize", 5));
    }

    /** Chỉ nhận đúng các mức có trên giao diện; giá trị lạ (kể cả rất lớn) rơi về mặc định. */
    static int normalizePageSize(int value) {
        return value == 10 || value == 20 || value == 50 ? value : 5;
    }

    /**
     * Bộ lọc loại hao hụt của nhật ký đi bằng tên "logType", không dùng chung "wasteType" với form ghi:
     * form ghi gửi nhiều giá trị wasteType (mỗi dòng một giá trị), lấy nhầm là nhật ký tự lọc sai.
     */
    private static String logTypeParam(HttpServletRequest req) {
        return allowedParam(req, "logType", "SPILL", "EXPIRED", "REMAKE", "OTHER");
    }

    /**
     * URL quay lại chính màn này kèm bộ lọc + trang nhật ký đang xem, dùng cho redirect sau POST (PRG).
     * Không có nó thì ghi/sửa/huỷ xong là văng về trang 1 và mất hết điều kiện đang lọc.
     */
    private static String selfUrlKeepingFilters(HttpServletRequest req, Integer forcePage) {
        return buildSelfUrl(req.getContextPath(), textParam(req, "q", 100), logTypeParam(req),
                allowedParam(req, "status", "ACTIVE", "VOIDED"), pageSizeParam(req),
                forcePage != null ? forcePage : positiveIntParam(req, "page", 1));
    }

    /** Phần thuần của {@link #selfUrlKeepingFilters} — tách ra để test được mà không cần dựng request. */
    static String buildSelfUrl(String contextPath, String query, String logType, String status, int pageSize, int page) {
        StringBuilder qs = new StringBuilder();
        appendParam(qs, "q", query);
        appendParam(qs, "logType", logType);
        appendParam(qs, "status", status);
        appendParam(qs, "pageSize", String.valueOf(pageSize));
        appendParam(qs, "page", String.valueOf(page));
        return contextPath + "/barista/waste" + (qs.length() == 0 ? "" : "?" + qs);
    }

    private static void appendParam(StringBuilder qs, String name, String value) {
        if (blank(value)) return;
        if (qs.length() > 0) qs.append('&');
        qs.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private record WasteLineKey(int ingredientId, String wasteType, String causeCode, String reason) { }

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
}
