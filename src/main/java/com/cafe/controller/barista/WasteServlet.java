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
import com.cafe.web.support.RequestParams;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * B5 · WasteServlet → /barista/waste. The counter's INGREDIENT waste-reporting screen: log/edit/void via the ledger.
 * Waste from remaking items is out of scope here — KDS logs it automatically and Manager reconciles it in the waste report.
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
        if (shiftSupport.guardWrite(req, resp, "/barista/waste")) return;   // off-shift → block writes
        String editId = null;

        try {
            if ("createIngredientWaste".equals(action)) {
                WasteBatchForm form = WasteBatchForm.from(req);
                // One pass builds both shapes: WasteRowForm to re-render the form on error,
                // WasteLineInput to pass down to the Service.
                List<WasteRowForm> submitted = new ArrayList<>(form.lines().size());
                List<WasteService.WasteLineInput> lines = new ArrayList<>(form.lines().size());
                for (WasteBatchForm.Line row : form.lines()) {
                    submitted.add(new WasteRowForm(row.ingredientId(), row.quantity(), row.wasteType(),
                            row.reasonPreset(), row.reasonDetail()));
                    lines.add(new WasteService.WasteLineInput(row.ingredientId(), row.quantity(),
                            row.wasteType(), row.reasonPreset(), row.reasonDetail()));
                }
                req.setAttribute("submittedWasteRows", submitted);
                req.setAttribute("wasteClientRequestId", form.clientRequestId());
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
            // A newly logged row sits at the top (LoggedAt DESC), so only going to page 1 shows it; edit/void keeps the current page.
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
            // On an infrastructure error, redirect safely; don't forward again to avoid a second broken DB query.
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
        String logQuery = RequestParams.text(req, "q", 100);
        String logWasteType = logTypeParam(req);
        String logStatus = RequestParams.allowed(req, "status", "ACTIVE", "VOIDED");
        int logPageSize = pageSizeParam(req);
        int requestedLogPage = RequestParams.positiveInt(req, "page", 1);

        // The overview keeps the full scope; the log table only fetches the current page from DB.
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
        shiftSupport.expose(req, "/barista/waste");   // on-shift: banner + block writes

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
            // The prefill params live in the URL, so a user can edit them; garbage values just fall back to an empty form.
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
        return RequestParams.isBlank(value);
    }

    /** The log defaults to 5 rows/page for easy tracking at the counter; barista can pick 10/20/50 when reviewing. */
    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(RequestParams.positiveInt(req, "pageSize", 5));
    }

    /** Only accepts the values present in the UI; unrecognized values (even very large ones) fall back to the default. */
    static int normalizePageSize(int value) {
        return value == 10 || value == 20 || value == 50 ? value : 5;
    }

    /**
     * The log's waste-type filter travels under the name "logType", not shared with the log form's "wasteType":
     * the log form submits multiple wasteType values (one per row), so reusing the name would filter the log incorrectly.
     *
     * <p>Only the three ingredient-waste types; REMAKE is out of scope for this screen, so even typing it
     * into the URL by hand gets ignored (falls back to "all" for the ingredient-waste section).
     */
    private static String logTypeParam(HttpServletRequest req) {
        return RequestParams.allowed(req, "logType", "SPILL", "EXPIRED", "OTHER");
    }

    /**
     * URL back to this same screen with the current filters + log page, used for the post-POST redirect (PRG).
     * Without it, logging/editing/voiding would bounce back to page 1 and lose all active filter conditions.
     */
    private static String selfUrlKeepingFilters(HttpServletRequest req, Integer forcePage) {
        return buildSelfUrl(req.getContextPath(), RequestParams.text(req, "q", 100), logTypeParam(req),
                RequestParams.allowed(req, "status", "ACTIVE", "VOIDED"), pageSizeParam(req),
                forcePage != null ? forcePage : RequestParams.positiveInt(req, "page", 1));
    }

    /** Pure part of {@link #selfUrlKeepingFilters} — factored out so it can be tested without building a request. */
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
