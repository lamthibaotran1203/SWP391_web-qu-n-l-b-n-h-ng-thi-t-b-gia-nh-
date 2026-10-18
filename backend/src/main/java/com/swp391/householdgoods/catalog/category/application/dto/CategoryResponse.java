package com.swp391.householdgoods.catalog.category.application.dto;

import com.swp391.householdgoods.catalog.category.domain.ProductCategory;
import java.time.Instant;

public record CategoryResponse(
        Long id,
        Long parentId,
        String parentName,
        String code,
        String name,
        String description,
        boolean isDeleted,
        String status,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static CategoryResponse from(ProductCategory category, String parentName) {
        return new CategoryResponse(
                category.getId(), category.getParentId(), parentName,
                category.getCode(), category.getName(), category.getDescription(),
                category.isDeleted(), category.isDeleted() ? "INACTIVE" : "ACTIVE",
                category.getCreatedBy(), category.getCreatedAt(),
                category.getUpdatedBy(), category.getUpdatedAt());
    }
}
