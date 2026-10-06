package com.swp391.householdgoods.orders.api;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesOrderSummary(
        Long id,
        String orderNumber,
        String channel,
        String status,
        String paymentStatus,
        String paymentMethod,
        String customerName,
        String customerEmail,
        String customerPhone,
        BigDecimal totalAmount,
        Instant placedAt) {
}
