package com.swp391.householdgoods.purchasing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "Mã nhà cung cấp không được để trống")
        @Size(max = 50, message = "Mã nhà cung cấp không quá 50 ký tự")
        String code,

        @NotBlank(message = "Mã số thuế không được để trống")
        @Size(max = 50, message = "Mã số thuế không quá 50 ký tự")
        String taxCode,

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
        String address
) {
}
