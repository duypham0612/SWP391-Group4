package com.cafe.web.form;

import com.cafe.common.BusinessDay;
import com.cafe.model.Voucher;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/** Mapper form voucher từ giờ tường Việt Nam sang dữ liệu UTC. */
public record VoucherForm(Voucher voucher) {
    public static VoucherForm from(HttpServletRequest request) {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(FormValues.optionalInt(request.getParameter("voucherId"), "Mã voucher"));
        String code = FormValues.trim(request.getParameter("code"));
        voucher.setCode(code == null ? null : code.toUpperCase(Locale.ROOT));
        voucher.setDiscountType(FormValues.trim(request.getParameter("discountType")));
        voucher.setDiscountValue(FormValues.decimal(request.getParameter("discountValue"), "Giá trị giảm"));
        voucher.setMinOrderAmount(FormValues.decimal(request.getParameter("minOrderAmount"), "Đơn tối thiểu"));
        bindScope(request.getParameter("scopeTarget"), voucher);
        voucher.setStartAtUtc(BusinessDay.toUtc(FormValues.optionalDateTime(
                request.getParameter("startAtLocal"), "Thời gian bắt đầu")));
        voucher.setEndAtUtc(BusinessDay.toUtc(FormValues.optionalDateTime(
                request.getParameter("endAtLocal"), "Thời gian kết thúc")));
        voucher.setUsageLimit(FormValues.optionalInteger(request.getParameter("usageLimit"), "Giới hạn sử dụng"));
        voucher.setActive(request.getParameter("active") != null);
        return new VoucherForm(voucher);
    }

    private static void bindScope(String raw, Voucher voucher) {
        String value = FormValues.trim(raw);
        if ("CHAIN".equals(value)) {
            voucher.setScope("CHAIN");
            voucher.setBranchId(null);
        } else if (value != null && value.startsWith("BRANCH:")) {
            voucher.setScope("BRANCH");
            int id = FormValues.optionalInt(value.substring("BRANCH:".length()), "Chi nhánh");
            voucher.setBranchId(id > 0 ? id : null);
        } else {
            voucher.setScope(value);
            voucher.setBranchId(null);
        }
    }
}
