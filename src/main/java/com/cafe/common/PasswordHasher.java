package com.cafe.common;

import org.mindrot.jbcrypt.BCrypt;

/** Băm & kiểm tra mật khẩu bằng BCrypt (đặc tả: common/security/PasswordHasher). */
public final class PasswordHasher {

    private PasswordHasher() { }

    public static String hashPassword(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(10));
    }

    public static boolean verifyPassword(String raw, String hash) {
        if (raw == null || hash == null || !hash.startsWith("$2")) {
            return false; // placeholder/hash hỏng -> không khớp
        }
        try {
            return BCrypt.checkpw(raw, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
