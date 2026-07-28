package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Voucher;
import com.cafe.service.admin.BranchService;
import com.cafe.service.shared.VoucherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

/** Admin voucher management. */
@WebServlet("/admin/voucher")
public class VoucherServlet extends HttpServlet {

    private static final Set<String> TYPES = Set.of("PERCENT", "FIXED");
    private static final Set<String> SCOPES = Set.of("CHAIN", "BRANCH");
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9_-]+");

    private final VoucherService service = new VoucherService();
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("voucher", new Voucher());
                forwardForm(req, resp, "Thêm voucher");
            } else if ("edit".equals(action)) {
                Voucher voucher = service.getVoucher(Integer.parseInt(req.getParameter("id")));
                if (voucher == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                req.setAttribute("voucher", voucher);
                forwardForm(req, resp, "Sửa voucher");
            } else {
                req.setAttribute("vouchers", service.getVoucherList());
                req.setAttribute("pageTitle", "Voucher");
                req.getRequestDispatcher("/WEB-INF/views/admin/voucher-list.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF");
            return;
        }

        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                toggleActive(req);
                resp.sendRedirect(ctx + "/admin/voucher");
                return;
            }

            Voucher voucher = bind(req);
            if (voucher.getVoucherId() != 0) {
                Voucher existing = service.getVoucher(voucher.getVoucherId());
                if (existing != null) {
                    voucher.setCode(existing.getCode());
                }
            }

            String error = validate(voucher);
            if (error != null) {
                req.setAttribute("voucher", voucher);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, voucher.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher");
                return;
            }

            if (voucher.getVoucherId() == 0) {
                service.createVoucher(voucher);
                req.getSession().setAttribute("flashOk", "Đã thêm voucher thành công.");
            } else {
                service.updateVoucher(voucher);
                req.getSession().setAttribute("flashOk", "Đã cập nhật voucher thành công.");
            }
            resp.sendRedirect(ctx + "/admin/voucher");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void toggleActive(HttpServletRequest req) throws Exception {
        Voucher voucher = service.getVoucher(Integer.parseInt(req.getParameter("id")));
        if (voucher == null) {
            req.getSession().setAttribute("flashError", "Không tìm thấy voucher.");
            return;
        }
        if (!"RUNNING".equals(voucher.getLifecycleStatusCode())) {
            req.getSession().setAttribute("flashError",
                    "Chỉ có thể bật hoặc tắt voucher đang diễn ra.");
            return;
        }
        service.toggleActive(voucher.getVoucherId());
        req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái voucher.");
    }

    private Voucher bind(HttpServletRequest req) {
        Voucher voucher = new Voucher();
        String id = req.getParameter("voucherId");
        if (id != null && !id.isBlank()) {
            voucher.setVoucherId(Integer.parseInt(id));
        }
        voucher.setCode(upper(req.getParameter("code")));
        voucher.setDiscountType(trim(req.getParameter("discountType")));
        voucher.setDiscountValue(decimal(req.getParameter("discountValue")));
        voucher.setMinOrderAmount(decimal(req.getParameter("minOrderAmount")));
        bindScope(req.getParameter("scopeTarget"), voucher);
        voucher.setStartDate(dateTime(req.getParameter("startDate")));
        voucher.setEndDate(dateTime(req.getParameter("endDate")));
        voucher.setUsageLimit(parseOptionalNonNegativeInt(req.getParameter("usageLimit")));
        voucher.setActive(req.getParameter("active") != null);
        return voucher;
    }

    private void bindScope(String raw, Voucher voucher) {
        String value = trim(raw);
        if ("CHAIN".equals(value)) {
            voucher.setScope("CHAIN");
            voucher.setBranchId(null);
            return;
        }
        if (value != null && value.startsWith("BRANCH:")) {
            int branchId = parsePositiveInt(value.substring("BRANCH:".length()));
            voucher.setScope("BRANCH");
            voucher.setBranchId(branchId <= 0 ? null : branchId);
            return;
        }
        voucher.setScope(value);
        voucher.setBranchId(null);
    }

    private String validate(Voucher voucher) throws Exception {
        if (voucher.getCode() == null || voucher.getCode().isBlank()) {
            return "Mã voucher không được để trống.";
        }
        if (voucher.getCode().length() > 40) {
            return "Mã voucher không được vượt quá 40 ký tự.";
        }
        if (!CODE_PATTERN.matcher(voucher.getCode()).matches()) {
            return "Mã voucher chỉ được chứa chữ cái không dấu, chữ số, dấu gạch ngang hoặc gạch dưới.";
        }
        if (service.isCodeInUse(voucher.getCode(), voucher.getVoucherId())) {
            return "Mã voucher đã tồn tại. Vui lòng chọn mã khác.";
        }
        if (voucher.getDiscountType() == null || !TYPES.contains(voucher.getDiscountType())) {
            return "Loại giảm phải là PERCENT hoặc FIXED.";
        }
        if (voucher.getDiscountValue() == null || voucher.getDiscountValue().signum() < 0) {
            return "Giá trị giảm phải lớn hơn hoặc bằng 0.";
        }
        if ("PERCENT".equals(voucher.getDiscountType())
                && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            return "Giảm theo phần trăm không được vượt quá 100.";
        }
        if (voucher.getScope() == null || !SCOPES.contains(voucher.getScope())) {
            return "Phạm vi áp dụng không hợp lệ.";
        }
        if ("BRANCH".equals(voucher.getScope()) && voucher.getBranchId() == null) {
            return "Vui lòng chọn chi nhánh áp dụng.";
        }
        if ("CHAIN".equals(voucher.getScope())) {
            voucher.setBranchId(null);
        }
        if (voucher.getUsageLimit() != null && voucher.getUsageLimit() < 0) {
            return "Giới hạn sử dụng phải lớn hơn hoặc bằng 0.";
        }
        if (voucher.getStartDate() != null && voucher.getEndDate() != null
                && !voucher.getEndDate().isAfter(voucher.getStartDate())) {
            return "Ngày kết thúc phải sau ngày bắt đầu.";
        }
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try {
            req.setAttribute("branches", branchService.getBranchListActive());
        } catch (Exception e) {
            throw new ServletException(e);
        }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/voucher-form.jsp").forward(req, resp);
    }

    private BigDecimal decimal(String raw) {
        try {
            return raw == null || raw.isBlank()
                    ? BigDecimal.ZERO
                    : new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(-1);
        }
    }

    private LocalDateTime dateTime(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : LocalDateTime.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private int parsePositiveInt(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return 0;
            }
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Integer parseOptionalNonNegativeInt(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            int value = Integer.parseInt(raw.trim());
            return value >= 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
