package com.swp391.householdgoods.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email quá dài.")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(
                min = 8,
                max = 72,
                message = "Mật khẩu phải có từ 8 đến 72 ký tự.")
        String password,

        @NotBlank(message = "Họ tên không được để trống.")
        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự.")
        String fullName
) {
}