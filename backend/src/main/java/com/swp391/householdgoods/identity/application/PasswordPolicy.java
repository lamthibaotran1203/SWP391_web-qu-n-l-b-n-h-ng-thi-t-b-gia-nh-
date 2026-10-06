package com.swp391.householdgoods.identity.application;

import java.nio.charset.StandardCharsets;

/** Shared policy approved for UC-05; never trim or normalize a password. */
public final class PasswordPolicy {
    public static final String MESSAGE = "Mật khẩu mới phải có từ 8 đến 64 ký tự.";

    private PasswordPolicy() {}

    public static String rejectionMessage(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "Mật khẩu mới quá dài. Hãy thử mật khẩu ngắn hơn.";
        }
        return MESSAGE;
    }

    public static boolean accepts(String password) {
        if (password == null || !StandardCharsets.UTF_8.newEncoder().canEncode(password)) {
            return false;
        }
        int characters = password.codePointCount(0, password.length());
        return characters >= 8 && characters <= 64
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
