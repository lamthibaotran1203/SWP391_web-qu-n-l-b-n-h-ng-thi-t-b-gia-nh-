package com.swp391.householdgoods.catalog.category.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Mã danh mục không được để trống")
        @Size(max = 50, message = "Mã danh mục không quá 50 ký tự")
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*", message = "Mã danh mục chỉ gồm chữ, số, gạch ngang hoặc gạch dưới")
        String code,
        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 255, message = "Tên danh mục không quá 255 ký tự")
        String name,
        @Size(max = 500, message = "Mô tả không quá 500 ký tự")
        String description,
        @Positive(message = "Danh mục cha không hợp lệ")
        Long parentId
) {
}
