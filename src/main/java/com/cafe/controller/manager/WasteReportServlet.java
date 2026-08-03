package com.cafe.controller.manager;

import com.cafe.common.BusinessDay;
import com.cafe.service.manager.WasteReportService;
import com.cafe.service.shared.InventoryService;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.model.User;
import com.cafe.web.support.RequestParams;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** M · WasteReportServlet → /manager/waste. Manager chỉ xem nhật ký hao hụt/làm lại. */
@WebServlet("/manager/waste")
public class WasteReportServlet extends HttpServlet {
    private final WasteReportService service;

    public WasteReportServlet() { this(new WasteReportService()); }
    WasteReportServlet(WasteReportService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getAttribute("combinedInventoryView") == null) {
            String query = req.getQueryString();
            resp.sendRedirect(req.getContextPath() + "/manager/reconciliation"
                    + (query == null || query.isBlank() ? "" : "?" + query));
            return;
        }
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        LocalDate todayVn = LocalDate.now(BusinessDay.VN_ZONE);
        WasteReportService.Range range = WasteReportService.resolveRange(
                req.getParameter("from"), req.getParameter("to"), todayVn);
        String logQuery = RequestParams.text(req, "q", 100);
        String logWasteType = RequestParams.allowed(req, "wasteType", "SPILL", "EXPIRED", "REMAKE", "OTHER");
        String logStatus = RequestParams.allowed(req, "status", "ACTIVE", "VOIDED");
        int requestedLogPage = RequestParams.positiveInt(req, "page", 1);

        try {
            InventoryService.WasteLogPage p = service.page(branchId, range,
                    logQuery, logWasteType, logStatus, requestedLogPage, pageSizeParam(req));
            req.setAttribute("summary", service.summarize(branchId, range));
            req.setAttribute("wasteLogPage", p);
            req.setAttribute("logs", p.getLogs());
            req.setAttribute("range", range);
            req.setAttribute("todayDate", todayVn);
            req.setAttribute("last7FromDate", todayVn.minusDays(6));
            req.setAttribute("last30FromDate", todayVn.minusDays(29));
            req.setAttribute("wasteLogQuery", logQuery);
            req.setAttribute("wasteLogWasteType", logWasteType);
            req.setAttribute("wasteLogStatus", logStatus);
            req.setAttribute("openReviews", service.openReviews(branchId));
            req.setAttribute("corrections", service.corrections(branchId, range));
            req.setAttribute("correctionsLimit", WasteReportService.MAX_CORRECTIONS);
            req.setAttribute("pageTitle", "Đối soát tồn và hao hụt");
            req.getRequestDispatcher("/WEB-INF/views/manager/waste.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        if (!"resolveReview".equals(req.getParameter("action"))) { resp.sendError(400); return; }
        User user = SessionUtil.currentUser(req);
        try {
            long id = Long.parseLong(req.getParameter("wasteEntryId"));
            boolean ok = service.resolveReview(com.cafe.web.support.BranchContext.requireBranchId(req), id,
                    user == null ? 0 : user.getUserId(), req.getParameter("note"));
            req.getSession().setAttribute(ok ? "flashOk" : "flashError", ok ? "Đã xác nhận ngoại lệ." : "Ngoại lệ đã được xử lý.");
            resp.sendRedirect(selfUrlKeepingFilters(req));
        } catch (NumberFormatException e) { resp.sendError(400); }
        catch (Exception e) { throw new ServletException(e); }
    }

    private static boolean blank(String value) {
        return RequestParams.isBlank(value);
    }

    /** Màn manager ưu tiên nhìn được nhiều dòng hơn quầy pha chế; đối soát dài thì chọn tới 100. */
    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(RequestParams.positiveInt(req, "pageSize", 10));
    }

    /** Chỉ nhận đúng các mức có trên giao diện; giá trị lạ (kể cả rất lớn) rơi về mặc định. */
    static int normalizePageSize(int value) {
        return value == 20 || value == 50 || value == 100 ? value : 10;
    }

    /**
     * URL quay lại màn đối soát kèm khoảng ngày + bộ lọc + trang đang xem, dùng cho redirect sau POST (PRG).
     * Không có nó thì xử lý xong một ngoại lệ là khoảng ngày và trang nhật ký bị reset về mặc định.
     */
    private static String selfUrlKeepingFilters(HttpServletRequest req) {
        return buildSelfUrl(req.getContextPath(), RequestParams.text(req, "from", 10), RequestParams.text(req, "to", 10),
                RequestParams.text(req, "q", 100), RequestParams.allowed(req, "wasteType", "SPILL", "EXPIRED", "REMAKE", "OTHER"),
                RequestParams.allowed(req, "status", "ACTIVE", "VOIDED"), pageSizeParam(req), RequestParams.positiveInt(req, "page", 1));
    }

    /** Phần thuần của {@link #selfUrlKeepingFilters} — tách ra để test được mà không cần dựng request. */
    static String buildSelfUrl(String contextPath, String from, String to, String query,
                               String wasteType, String status, int pageSize, int page) {
        StringBuilder qs = new StringBuilder();
        appendParam(qs, "from", from);
        appendParam(qs, "to", to);
        appendParam(qs, "q", query);
        appendParam(qs, "wasteType", wasteType);
        appendParam(qs, "status", status);
        appendParam(qs, "pageSize", String.valueOf(pageSize));
        appendParam(qs, "page", String.valueOf(page));
        return contextPath + "/manager/reconciliation" + (qs.length() == 0 ? "" : "?" + qs);
    }

    private static void appendParam(StringBuilder qs, String name, String value) {
        if (blank(value)) return;
        if (qs.length() > 0) qs.append('&');
        qs.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
