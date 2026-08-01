package com.cafe.web.form;

import com.cafe.model.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

/** Mapper cú pháp của form nhân sự; invariant được kiểm tra lại tại UserService. */
public record UserForm(User user, String password, int assignmentBranchId) {
    public static UserForm from(HttpServletRequest request) {
        User user = new User();
        user.setUserId(FormValues.optionalInt(request.getParameter("userId"), "Mã nhân viên"));
        user.setUsername(lower(request.getParameter("username")));
        user.setFullName(FormValues.trim(request.getParameter("fullName")));
        user.setEmail(lower(request.getParameter("email")));
        user.setPhone(FormValues.trim(request.getParameter("phone")));
        user.setRoleCode(FormValues.trim(request.getParameter("roleCode")));
        int branchId = FormValues.optionalInt(request.getParameter("branchId"), "Chi nhánh");
        user.setBranchId(branchId > 0 ? branchId : null);
        String hourlyRate = FormValues.trim(request.getParameter("hourlyRate"));
        user.setHourlyRate(hourlyRate == null || hourlyRate.isBlank()
                ? null : FormValues.decimal(hourlyRate, "Lương theo giờ"));
        String status = FormValues.trim(request.getParameter("status"));
        user.setStatus(status == null || status.isBlank() ? "ACTIVE" : status);
        int assignmentBranchId = FormValues.optionalInt(
                request.getParameter("assignmentBranchId"), "Chi nhánh phân công");
        return new UserForm(user, request.getParameter("password"), assignmentBranchId);
    }

    private static String lower(String value) {
        String cleaned = FormValues.trim(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}
