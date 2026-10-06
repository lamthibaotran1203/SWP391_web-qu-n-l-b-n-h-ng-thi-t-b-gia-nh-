package com.swp391.householdgoods.catalog.api;

import java.util.List;

/** UC-09 public metadata and eligible manual sale units, without costs or audit actors. */
public record CatalogProductDetail(long productId, String name, String description,
        long categoryId, String categoryName, long brandId, String brandName,
        List<String> images, List<CatalogSku> skus) {
    public CatalogProductDetail {
        images = List.copyOf(images);
        skus = List.copyOf(skus);
    }
}
