package com.swp391.householdgoods.identity.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotNull(message = "Nhập mật khẩu hiện tại.")
        @Size(max = 144, message = "Mật khẩu hiện tại quá dài.")
        String currentPassword,
        @NotNull(message = "Nhập mật khẩu mới.")
        @Size(max = 144, message = "Mật khẩu mới quá dài.")
        String newPassword,
        @NotNull(message = "Nhập xác nhận mật khẩu mới.")
        @Size(max = 144, message = "Xác nhận mật khẩu quá dài.")
        String confirmation) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[REDACTED]";
    }
}
