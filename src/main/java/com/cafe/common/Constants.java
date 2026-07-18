package com.cafe.common;

import java.math.BigDecimal;

/** Hằng số dùng chung: session key, role code, request attribute. */
public final class Constants {

    private Constants() { }

    // Session keys
    public static final String SESSION_USER = "authUser";
    public static final String SESSION_CSRF = "csrfToken";
    public static final String SESSION_DRAFT_CARTS = "draftCarts";

    // Request attributes
    public static final String ATTR_BRANCH_ID = "branchId";

    // Role codes (khớp iam.Role.Code)
    public static final String ROLE_ADMIN   = "ADMIN";
    public static final String ROLE_MANAGER = "BRANCH_MANAGER";
    public static final String ROLE_CASHIER = "CASHIER";
    public static final String ROLE_BARISTA = "BARISTA";

    // KDS SLA thresholds (hàng chờ pha)
    public static final int KDS_WARN_SECONDS = 8 * 60;
    public static final int KDS_CRIT_SECONDS = 12 * 60;
    // Mốc pha chuẩn mặc định khi món chưa khai PrepSeconds (giữ hành vi cũ 12 phút).
    public static final int KDS_SLA_SECONDS = KDS_CRIT_SECONDS;

    // Pickup SLA thresholds (món pha xong chờ giao — kể từ DoneAt)
    public static final int PICKUP_WARN_SECONDS = 3 * 60;
    public static final int PICKUP_CRIT_SECONDS = 6 * 60;

    // Ngưỡng cao điểm mặc định (số ly đang chờ+đang pha) khi chi nhánh chưa đặt riêng.
    // Vượt ngưỡng → bảng chuyển sang xếp thứ tự thay vì tô đỏ hàng loạt.
    public static final int PEAK_THRESHOLD_CUPS = 12;

    // Payroll
    public static final BigDecimal MIN_HOURLY_RATE = new BigDecimal("25000");

    // Demo VietQR account for offline QR rendering.
    public static final String VIETQR_BANK_BIN = "970422";
    public static final String VIETQR_ACCOUNT_NO = "1234567890";
    public static final String VIETQR_ACCOUNT_NAME = "CAFE CHAIN";
}
