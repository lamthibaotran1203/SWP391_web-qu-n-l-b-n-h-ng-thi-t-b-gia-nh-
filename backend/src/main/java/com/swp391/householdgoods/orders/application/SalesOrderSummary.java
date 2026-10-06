package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.domain.OrderChannel;
import com.swp391.householdgoods.orders.domain.OrderStatus;
import com.swp391.householdgoods.orders.domain.PaymentMethod;
import com.swp391.householdgoods.orders.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record SalesOrderSummary(
        Long id,
        String orderNumber,
        OrderChannel channel,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        String customerName,
        String customerEmail,
        String customerPhone,
        BigDecimal totalAmount,
        Instant placedAt) {
}
