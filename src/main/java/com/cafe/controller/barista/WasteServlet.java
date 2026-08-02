package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.web.support.BaristaShiftSupport;
import com.cafe.web.support.BaristaWritePolicy;
import com.cafe.model.User;
import com.cafe.model.WasteEventItem;
import com.cafe.service.barista.WasteService;
import com.cafe.service.shared.InventoryService;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.WasteBatchForm;
import com.cafe.web.support.BranchContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * B5 · WasteServlet → /barista/waste. Màn báo hao hụt NGUYÊN LIỆU của quầy: ghi/sửa/huỷ qua ledger.
 * Hao hụt do làm lại món không thuộc màn này — KDS ghi tự động và Quản lý đối soát ở báo cáo hao hụt.
 */
@WebServlet("/barista/waste")
public class WasteServlet extends HttpServlet {

    private final WasteService service;
    private final BaristaShiftSupport shiftSupport;
    public WasteServlet() {
        this(new WasteService(), new BaristaShiftSupport());
    }

    WasteServlet(WasteService service, BaristaShiftSupport shiftSupport) {
        this.service = Objects.requireNonNull(service, "service");
        this.shiftSupport = Objects.requireNonNull(shiftSupport, "shiftSupport");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = BranchContext.requireBranchId(req);
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
        int branchId = BranchContext.requireBranchId(req);
        int userId = currentUserId(req);
        String action = req.getParameter("action");
        if (!BaristaWritePolicy.isWasteAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(req.getContextPath() + "/barista/waste");
            return;
        }
        if (shiftSupport.guardWrite(req, resp, "/barista/waste")) return;   // ngoài ca → chặn ghi
        String editId = null;

        try {
            if ("createIngredientWaste".equals(action)) {
                WasteBatchForm form = WasteBatchForm.from(req);
                List<WasteRowForm> submitted = form.lines().stream()
                        .map(row -> new WasteRowForm(row.ingredientId(), row.quantity(), row.wasteType(),
                                row.reasonPreset(), row.reasonDetail()))
                        .toList();
                req.setAttribute("submittedWasteRows", submitted);
                req.setAttribute("wasteClientRequestId", form.clientRequestId());
                List<WasteService.WasteLineInput> lines = form.lines().stream()
                        .map(row -> new WasteService.WasteLineInput(row.ingredientId(), row.quantity(),
                                row.wasteType(), row.reasonPreset(), row.reasonDetail()))
                        .toList();
                int count = service.logIngredientWasteBatch(branchId,
                        new WasteService.WasteBatchCommand(form.clientRequestId(), lines), userId);
                req.getSession().setAttribute("flashOk", count == 0 ? "Yêu cầu này đã được ghi trước đó." : "Đã ghi " + count + " dòng hao hụt.");
            } else if ("update".equals(action)) {
                editId = req.getParameter("wasteEntryId");
                req.setAttribute("editQuantity", req.getParameter("quantity"));
                req.setAttribute("editWasteType", req.getParameter("wasteType"));
                req.setAttribute("editReason", req.getParameter("reason"));
                long wasteEntryId = parseLong(editId, "Bản ghi cần sửa không hợp lệ.");
                BigDecimal qty = parseQty(req.getParameter("quantity"), "Số lượng phải > 0.");
                service.updateWaste(branchId, wasteEntryId, qty, req.getParameter("wasteType"), req.getParameter("reason"), userId);
                req.getSession().setAttribute("flashOk", "Đã sửa — chênh lệch ghi vào sổ cái.");
            } else if ("void".equals(action)) {
                long wasteEntryId = parseLong(req.getParameter("wasteEntryId"), "Bản ghi cần huỷ không hợp lệ.");
                service.voidWaste(branchId, wasteEntryId, userId);
                req.getSession().setAttribute("flashOk", "Đã huỷ — tồn kho hoàn lại qua sổ cái (txn bù).");
            } else {
                throw new BusinessException("Thao tác không hợp lệ.");
            }
            // Dòng vừa ghi nằm trên cùng (LoggedAt DESC) nên về trang 1 mới thấy; sửa/huỷ thì giữ nguyên trang.
            resp.sendRedirect(selfUrlKeepingFilters(req, "createIngredientWaste".equals(action) ? 1 : null));
        } catch (BusinessException | FormBindingException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (NumberFormatException e) {
            req.setAttribute("flashError", "Dữ liệu số không hợp lệ.");
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (IllegalArgumentException e) {
            req.setAttribute("flashError", e.getMessage());
            forwardAfterError(req, resp, branchId, userId, editId);
        } catch (Exception e) {
            // Hạ tầng lỗi thì redirect an toàn; không forward lại để tránh truy vấn DB hỏng lần thứ hai.
            req.getSession().setAttribute("flashError", "Không thể cập nhật hao hụt lúc này. Vui lòng thử lại.");
            resp.sendRedirect(selfUrlKeepingFilters(req, null));
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
            throws Exception {
        WasteService.WasteScope scope = service.resolveScope(userId, branchId);
        String logQuery = textParam(req, "q", 100);
        String logWasteType = logTypeParam(req);
        String logStatus = allowedParam(req, "status", "ACTIVE", "VOIDED");
        int logPageSize = pageSizeParam(req);
        int requestedLogPage = positiveIntParam(req, "page", 1);

        // Tổng quan giữ nguyên toàn bộ phạm vi; bảng nhật ký thì chỉ lấy đúng trang từ DB.
        List<WasteEventItem> scopedLogs = service.getWasteLogs(branchId, scope);
        InventoryService.WasteLogPage wasteLogPage = service.getWasteLogPage(branchId, scope,
                logQuery, logWasteType, logStatus, requestedLogPage, logPageSize);
        req.setAttribute("ingredients", service.getIngredients(branchId));
        req.setAttribute("scope", scope);
        req.setAttribute("logs", wasteLogPage.getLogs());
        req.setAttribute("wasteLogPage", wasteLogPage);
        req.setAttribute("wasteLogQuery", logQuery);
        req.setAttribute("wasteLogWasteType", logWasteType);
        req.setAttribute("wasteLogStatus", logStatus);
        req.setAttribute("summary", service.summarize(scopedLogs));
        req.setAttribute("pageTitle", "Hao hụt nguyên liệu");
        req.setAttribute("currentUserId", userId);
        shiftSupport.expose(req, "/barista/waste");   // trực ca: banner + khoá thao tác

        if (req.getAttribute("submittedWasteRows") == null) {
            req.setAttribute("submittedWasteRows", List.of(new WasteRowForm("", "", "SPILL", "", "")));
        }
        if (req.getAttribute("wasteClientRequestId") == null) {
            req.setAttribute("wasteClientRequestId", UUID.randomUUID().toString());
        }
        if (editId != null && !editId.isBlank()) {
            try {
                WasteEventItem editLog = service.getEditableWasteLog(branchId, Long.parseLong(editId), userId);
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
            // Tham số prefill nằm trên URL nên người dùng sửa được; giá trị rác thì rơi về form trống.
        }
    }

    private static long parseLong(String value, String message) {
        if (blank(value)) throw new BusinessException(message);
        try { return Long.parseLong(value.trim()); }
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
     *
     * <p>Chỉ ba loại của hao hụt nguyên liệu; REMAKE không nằm trong phạm vi màn này nên có gõ tay
     * vào URL cũng bị bỏ qua (rơi về "tất cả" của phần hao hụt nguyên liệu).
     */
    private static String logTypeParam(HttpServletRequest req) {
        return allowedParam(req, "logType", "SPILL", "EXPIRED", "OTHER");
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
