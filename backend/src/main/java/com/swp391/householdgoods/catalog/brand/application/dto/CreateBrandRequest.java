package com.swp391.householdgoods.catalog.brand.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(
        @NotBlank(message = "Tên thương hiệu không được để trống")
        @Size(max = 255, message = "Tên thương hiệu không quá 255 ký tự")
        String name,

        @Size(max = 100, message = "Quốc gia không quá 100 ký tự")
        String country,

        @Size(max = 500, message = "Mô tả không quá 500 ký tự")
        String description,

        @Size(max = 512, message = "Đường dẫn logo không quá 512 ký tự")
        String logoUrl
) {
}
