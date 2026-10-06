package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.domain.OrderChannel;
import com.swp391.householdgoods.orders.domain.OrderStatus;
import com.swp391.householdgoods.orders.domain.PaymentMethod;
import com.swp391.householdgoods.orders.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesOrderDetails(
        Long id,
        String orderNumber,
        OrderChannel channel,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
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
