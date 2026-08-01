package com.cafe.web.form;

import com.cafe.model.Branch;
import jakarta.servlet.http.HttpServletRequest;

/** Mapper cú pháp của form chi nhánh. */
public record BranchForm(Branch branch) {
    public static BranchForm from(HttpServletRequest request) {
        Branch branch = new Branch();
        branch.setBranchId(FormValues.optionalInt(request.getParameter("branchId"), "Mã chi nhánh"));
        branch.setName(FormValues.trim(request.getParameter("name")));
        branch.setAddress(FormValues.trim(request.getParameter("address")));
        branch.setPhone(null);
        branch.setActive(request.getParameter("active") != null);
        branch.setOpenTime(FormValues.optionalTime(request.getParameter("openTime"), "Giờ mở cửa"));
        branch.setCloseTime(FormValues.optionalTime(request.getParameter("closeTime"), "Giờ đóng cửa"));
        return new BranchForm(branch);
    }
}
