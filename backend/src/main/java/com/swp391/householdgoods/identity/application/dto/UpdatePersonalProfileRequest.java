package com.swp391.householdgoods.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePersonalProfileRequest(
        @NotBlank(message = "Họ và tên không được để trống.")
        @Size(min = 2, max = 100, message = "Họ và tên cần từ 2–100 ký tự.")
        String fullName,
        @Size(max = 50, message = "Số điện thoại chưa hợp lệ.")
        String phone) {}
