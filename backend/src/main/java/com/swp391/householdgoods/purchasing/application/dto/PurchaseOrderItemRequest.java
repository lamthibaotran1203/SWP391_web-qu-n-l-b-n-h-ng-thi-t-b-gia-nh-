package com.swp391.householdgoods.purchasing.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseOrderItemRequest(
        @NotNull(message = "Mã sản phẩm (SKU ID) không được để trống")
        Long skuId,

        @NotNull(message = "Số lượng đặt hàng không được để trống")
        @Min(value = 1, message = "Số lượng đặt hàng phải lớn hơn 0")
        Integer orderedQuantity,

        @NotNull(message = "Đơn giá nhập không được để trống")
        @DecimalMin(value = "0", message = "Đơn giá nhập không được âm")
        BigDecimal unitCost
) {
}
