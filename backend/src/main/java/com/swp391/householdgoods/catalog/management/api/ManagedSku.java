package com.swp391.householdgoods.catalog.management.api;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import java.util.Map;

public record ManagedSku(
        long skuId, long productId, String skuCode, String barcode, String skuName,
        Map<String, String> attributes, String imageUrl, String originalPrice, String sellingPrice,
        int minStockThreshold, ProductStatus status, long skuVersion,
        int availableQuantity, boolean lowStock) {}
