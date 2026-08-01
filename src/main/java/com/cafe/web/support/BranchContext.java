package com.cafe.web.support;

import com.cafe.common.Constants;
import com.cafe.model.User;
import jakarta.servlet.http.HttpServletRequest;

/** Một điểm duy nhất phân giải chi nhánh hiệu lực cho request web. */
public final class BranchContext {
    public static final int LOCAL_DEMO_BRANCH_ID = 1;

    private BranchContext() { }

    public static int requireBranchId(HttpServletRequest request) {
        Object scoped = request.getAttribute(Constants.ATTR_BRANCH_ID);
        if (scoped instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }

        User user = SessionUtil.currentUser(request);
        if (user != null && user.getBranchId() != null && user.getBranchId() > 0) {
            return user.getBranchId();
        }

        String requested = request.getParameter("branchId");
        if (requested == null || requested.isBlank()) return LOCAL_DEMO_BRANCH_ID;
        try {
            int branchId = Integer.parseInt(requested.trim());
            return branchId > 0 ? branchId : LOCAL_DEMO_BRANCH_ID;
        } catch (NumberFormatException ignored) {
            return LOCAL_DEMO_BRANCH_ID;
        }
    }
}
