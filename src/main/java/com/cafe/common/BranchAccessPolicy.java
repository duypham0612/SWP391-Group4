package com.cafe.common;

/** Shared access rule for staff assigned to a branch. */
public final class BranchAccessPolicy {

    public static final String BRANCH_STOPPED_MESSAGE =
            "Chi nhánh đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.";
    public static final String BRANCH_UNMANAGED_MESSAGE =
            "Chi nhánh chưa có quản lý. Vui lòng thêm quản lý để nhân sự hoạt động bình thường.";

    private BranchAccessPolicy() {}

    public static String blockedMessage(Boolean active, Boolean managerAssigned) {
        if (!Boolean.TRUE.equals(active)) return BRANCH_STOPPED_MESSAGE;
        if (!Boolean.TRUE.equals(managerAssigned)) return BRANCH_UNMANAGED_MESSAGE;
        return null;
    }
}
