package com.swp391.householdgoods.catalog.application.dto;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.api.CatalogSku;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String brand,
        String category,
        BigDecimal price,
        int stockQuantity,
        String description,
        String imageUrl,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long productId,
        Long productVersion,
        Long skuVersion) {
    public static ProductResponse from(CatalogSku sku) {
        return new ProductResponse(
                sku.skuId(),
                sku.skuCode(),
                sku.productName(),
                sku.brandName(),
                sku.categoryName(),
                new BigDecimal(sku.sellingPrice()),
                sku.availableQuantity(),
                null,
                sku.imageUrl(),
                ProductStatus.ACTIVE,
                null,
                null, sku.productId(), sku.productVersion(), sku.skuVersion());
    }
}
