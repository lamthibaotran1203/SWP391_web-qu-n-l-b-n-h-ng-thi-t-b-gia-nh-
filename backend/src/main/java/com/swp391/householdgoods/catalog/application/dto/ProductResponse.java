package com.swp391.householdgoods.catalog.application.dto;

import com.swp391.householdgoods.catalog.domain.Product;
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
        Instant updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getBrand(), product.getCategory(),
                product.getPrice(), product.getStockQuantity(), product.getDescription(), product.getImageUrl(),
                product.getStatus(), product.getCreatedAt(), product.getUpdatedAt());
    }

    public static ProductResponse from(CatalogSku sku) {
        return new ProductResponse(
                sku.skuId(),
                sku.skuCode(),
                sku.skuName() == null || sku.skuName().isBlank() ? sku.productName() : sku.skuName(),
                sku.brandName(),
                sku.categoryName(),
                new BigDecimal(sku.sellingPrice()),
                sku.availableQuantity(),
                null,
                sku.imageUrl(),
                sku.purchasable() ? ProductStatus.ACTIVE : ProductStatus.INACTIVE,
                null,
                null);
    }
}
