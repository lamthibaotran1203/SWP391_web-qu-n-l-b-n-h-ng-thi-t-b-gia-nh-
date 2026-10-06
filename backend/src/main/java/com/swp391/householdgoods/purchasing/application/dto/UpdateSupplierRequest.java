package com.swp391.householdgoods.purchasing.application.dto;

import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
        @NotBlank(message = "Tên nhà cung cấp không được để trống")
        @Size(max = 255, message = "Tên nhà cung cấp không quá 255 ký tự")
        String name,

        @Size(max = 255, message = "Tên người liên hệ không quá 255 ký tự")
        String contactName,

        @Size(max = 50, message = "Số điện thoại không quá 50 ký tự")
        String phone,

        @Size(max = 255, message = "Email không quá 255 ký tự")
        String email,

        @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
        String address,

        @NotNull(message = "Trạng thái không được để trống")
        SupplierStatus status
) {
}
