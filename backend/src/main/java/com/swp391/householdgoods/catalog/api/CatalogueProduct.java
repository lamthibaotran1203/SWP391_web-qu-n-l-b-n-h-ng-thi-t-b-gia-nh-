package com.swp391.householdgoods.catalog.api;

public record CatalogueProduct(long productId, String name, String imageUrl,
        long categoryId, String categoryName, long brandId, String brandName,
        String minPrice, String maxPrice, long availableQuantity, int skuCount) {}
