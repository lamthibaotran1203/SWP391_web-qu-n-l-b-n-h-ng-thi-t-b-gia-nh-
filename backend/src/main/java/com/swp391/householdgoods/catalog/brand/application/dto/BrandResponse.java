package com.swp391.householdgoods.catalog.brand.application.dto;

import com.swp391.householdgoods.catalog.brand.domain.Brand;
import java.time.Instant;

public record BrandResponse(
        Long id,
        String name,
        String country,
        String description,
        String logoUrl,
        boolean isDeleted,
        String status,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {
    public static BrandResponse fromDomain(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getCountry(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.isDeleted(),
                brand.isDeleted() ? "INACTIVE" : "ACTIVE",
                brand.getCreatedBy(),
                brand.getCreatedAt(),
                brand.getUpdatedBy(),
                brand.getUpdatedAt()
        );
    }
}
