package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.domain.OrderChannel;
import java.time.LocalDate;

public record SalesOrderSearch(
        String query,
        OrderChannel channel,
        String status,
        String paymentStatus,
        LocalDate from,
        LocalDate to) {
}
