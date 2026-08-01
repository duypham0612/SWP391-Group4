package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.web.support.BaristaShiftSupport;
import com.cafe.web.support.BaristaWritePolicy;
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
import java.util.List;
import java.util.Objects;

/** B4 · PrepServlet → /barista/prep. Pha sẵn (RAW→PREPPED, Contract #2) + checklist + guard tồn. */
@WebServlet("/barista/prep")
public class PrepServlet extends HttpServlet {

    private final PrepService service;
    private final BaristaShiftSupport shiftSupport;

    public PrepServlet() {
        this(new PrepService(), new BaristaShiftSupport());
    }

    PrepServlet(PrepService service, BaristaShiftSupport shiftSupport) {
        this.service = Objects.requireNonNull(service, "service");
        this.shiftSupport = Objects.requireNonNull(shiftSupport, "shiftSupport");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
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
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        if (!BaristaWritePolicy.isPrepAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
            return;
        }
        if (shiftSupport.guardWrite(req, resp, "/barista/prep")) return;   // ngoài ca → chặn ghi
        try {
            if ("createBatch".equals(action)) {
                int ingredientId = positiveIntParam(req, "preppedIngredientId", 0);
                if (ingredientId <= 0) throw new BusinessException("Chưa chọn nguyên liệu pha sẵn.");
                String rawQty = req.getParameter("quantityProduced");
                if (blank(rawQty)) throw new BusinessException("Chưa nhập sản lượng thực tế.");
                BigDecimal qty = new BigDecimal(rawQty.trim());
                if (qty.signum() <= 0) throw new BusinessException("Sản lượng thực tế phải lớn hơn 0.");
                com.cafe.model.PrepBatch created = service.createSuggestedBatch(branchId, ingredientId, qty, userId,
                        req.getParameter("clientRequestId"));
                req.getSession().setAttribute("flashOk", created.isPending()
                        ? "Đã ghi nhận nguyên liệu đã dùng — sản lượng vượt mức thông thường nên cần Manager "
                          + "duyệt trước khi tính vào tồn kho bán được."
                        : "Đã xác nhận mẻ pha — tồn kho đã cập nhật.");
                resp.sendRedirect(req.getContextPath() + "/barista/prep");
                return;
            } else if ("writeOffExpired".equals(action)) {
                int batchId = Integer.parseInt(req.getParameter("prepBatchId"));
                service.writeOffExpiredBatchSuggested(branchId, batchId, userId);
                req.getSession().setAttribute("flashOk",
                        "Đã loại bỏ phần pha sẵn quá hạn — tồn kho đã cập nhật.");
            }
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (BusinessException e) {                         // vi phạm nghiệp vụ → flash, không 500
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Sản lượng không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (Exception e) {
            // SQL lỗi thật (timeout/mất kết nối) không được biến thành stack trace hoặc lộ dữ liệu nội bộ.
            req.getSession().setAttribute("flashError", "Không thể cập nhật mẻ pha lúc này. Vui lòng thử lại.");
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        }
    }

    private void forwardPage(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws Exception {
        List<Ingredient> prepped = service.getPreppedIngredients();
        List<com.cafe.model.PrepBatch> expiredBatches = service.getExpiredActiveBatches(branchId);

        req.setAttribute("preppedIngredients", prepped);
        req.setAttribute("checklist", service.getPrepChecklist(branchId));
        req.setAttribute("expiredBatches", expiredBatches);
        req.setAttribute("expiredBatchCount", expiredBatches.size());
        req.setAttribute("recentBatches", service.getRecentBatches(branchId));
        req.setAttribute("recipeJson", service.getRecipeJson(prepped));
        req.setAttribute("rawOnHandJson", service.getRawOnHandJson(branchId));
        req.setAttribute("clientRequestId", java.util.UUID.randomUUID().toString());
        req.setAttribute("pageTitle", "Pha sẵn nguyên liệu");
        shiftSupport.expose(req, "/barista/prep");   // trực ca: banner + khoá thao tác
            req.getRequestDispatcher("/WEB-INF/views/barista/prep.jsp").forward(req, resp);
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static int positiveIntParam(HttpServletRequest req, String name, int fallback) {
        try {
            int value = Integer.parseInt(req.getParameter(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
