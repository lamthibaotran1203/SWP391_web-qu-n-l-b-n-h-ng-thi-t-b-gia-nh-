package com.swp391.householdgoods.orders.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesOrderDetails(
        Long id,
        String orderNumber,
        String channel,
        String status,
        String paymentStatus,
        String paymentMethod,
        String customerName,
        String customerEmail,
        String customerPhone,
        String shippingAddress,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Instant placedAt,
        List<SalesOrderLineDetails> items) {
}
