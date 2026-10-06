package com.swp391.householdgoods.catalog.management.api;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import java.util.List;

public record ManagedProduct(
        long productId, String name, String description, Long categoryId, Long brandId,
        String imageUrl, ProductStatus status, long productVersion, List<ManagedSku> skus) {}
