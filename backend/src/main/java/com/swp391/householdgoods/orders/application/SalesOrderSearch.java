package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.domain.OrderChannel;
import com.swp391.householdgoods.orders.domain.OrderStatus;
import com.swp391.householdgoods.orders.domain.PaymentStatus;
import java.time.LocalDate;

public record SalesOrderSearch(
        String query,
        OrderChannel channel,
        OrderStatus status,
        PaymentStatus paymentStatus,
        LocalDate from,
        LocalDate to) {
}
