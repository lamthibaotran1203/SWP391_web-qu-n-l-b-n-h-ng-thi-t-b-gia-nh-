package com.swp391.householdgoods.orders.application;

public class SalesOrderStatusConflictException extends RuntimeException {

    public SalesOrderStatusConflictException(long id, String status) {
        super("Sales order " + id + " cannot be confirmed from status " + status);
    }
}
