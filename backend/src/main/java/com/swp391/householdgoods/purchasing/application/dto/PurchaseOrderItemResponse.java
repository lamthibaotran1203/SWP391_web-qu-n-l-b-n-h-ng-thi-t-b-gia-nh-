package com.swp391.householdgoods.purchasing.application.dto;

import com.swp391.householdgoods.purchasing.domain.PurchaseOrderItem;
import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long id,
        Long skuId,
        String skuCode,
        String skuName,
        int orderedQuantity,
        int receivedQuantity,
        BigDecimal unitCost,
        BigDecimal subtotal
) {
    public static PurchaseOrderItemResponse fromDomain(PurchaseOrderItem item, String skuCode, String skuName) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                item.getSkuId(),
                skuCode,
                skuName,
                item.getOrderedQuantity(),
                item.getReceivedQuantity(),
                item.getUnitCost(),
                item.getSubtotal()
        );
    }
}
