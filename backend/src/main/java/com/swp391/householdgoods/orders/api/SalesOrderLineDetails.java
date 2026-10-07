package com.swp391.householdgoods.orders.api;

import java.math.BigDecimal;

public record SalesOrderLineDetails(
        Long id,
        String productName,
        String productSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
