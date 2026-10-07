package com.swp391.householdgoods.purchasing.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull(message = "Nhà cung cấp không được để trống")
        Long supplierId,

        String expectedDate,

        @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
        String note,

        boolean isDraft,

        @NotEmpty(message = "Đơn đặt hàng phải có ít nhất một mặt hàng (PO Item)")
        @Valid
        List<PurchaseOrderItemRequest> items
) {
}
