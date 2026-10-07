package com.swp391.householdgoods.purchasing.application.dto;

import com.swp391.householdgoods.purchasing.domain.PurchaseOrder;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String poCode,
        Long supplierId,
        String supplierCode,
        String supplierName,
        Instant orderDate,
        Instant expectedDate,
        PurchaseOrderStatus status,
        BigDecimal totalCost,
        String note,
        Instant createdAt,
        List<PurchaseOrderItemResponse> items
) {
    public static PurchaseOrderResponse fromDomain(PurchaseOrder po, List<PurchaseOrderItemResponse> items) {
        return new PurchaseOrderResponse(
                po.getId(),
                po.getPoCode(),
                po.getSupplier().getId(),
                po.getSupplier().getCode(),
                po.getSupplier().getName(),
                po.getOrderDate(),
                po.getExpectedDate(),
                po.getStatus(),
                po.getTotalCost(),
                po.getNote(),
                po.getCreatedAt(),
                items
        );
    }
}
