package com.swp391.householdgoods.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(

        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 254, message = "Email quá dài.")
        String email

) {
}