package com.swp391.householdgoods.catalog.api;


/** Safe shared sale-unit projection; no internal costs, audit actors or legacy values. */
public record CatalogSku(long skuId, long productId, String productName, String skuCode,
        String skuName, String imageUrl, long categoryId, String categoryName,
        long brandId, String brandName, String sellingPrice, int availableQuantity,
        boolean purchasable, long productVersion, long skuVersion) {}
