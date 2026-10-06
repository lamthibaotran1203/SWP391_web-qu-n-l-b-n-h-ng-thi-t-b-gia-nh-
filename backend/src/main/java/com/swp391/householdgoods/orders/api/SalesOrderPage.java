package com.swp391.householdgoods.orders.api;

import java.util.List;

public record SalesOrderPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
