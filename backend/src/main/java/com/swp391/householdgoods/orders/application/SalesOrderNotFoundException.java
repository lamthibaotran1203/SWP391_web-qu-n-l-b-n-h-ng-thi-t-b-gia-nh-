package com.swp391.householdgoods.orders.application;

public class SalesOrderNotFoundException extends RuntimeException {

    public SalesOrderNotFoundException(long id) {
        super("Sales order not found: " + id);
    }
}
