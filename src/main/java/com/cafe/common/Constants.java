package com.cafe.common;

/** Hằng số dùng chung: session key, role code, request attribute. */
public final class Constants {

    private Constants() { }

    // Session keys
    public static final String SESSION_USER = "authUser";
    public static final String SESSION_CSRF = "csrfToken";

    // Request attributes
    public static final String ATTR_BRANCH_ID = "branchId";

    // Role codes (khớp iam.Role.Code)
    public static final String ROLE_ADMIN   = "ADMIN";
    public static final String ROLE_MANAGER = "BRANCH_MANAGER";
    public static final String ROLE_CASHIER = "CASHIER";
    public static final String ROLE_BARISTA = "BARISTA";
}
