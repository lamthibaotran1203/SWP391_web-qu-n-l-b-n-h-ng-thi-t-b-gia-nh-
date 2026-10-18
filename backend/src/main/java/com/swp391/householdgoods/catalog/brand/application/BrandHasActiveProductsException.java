package com.swp391.householdgoods.catalog.brand.application;

public class BrandHasActiveProductsException extends RuntimeException {
    public BrandHasActiveProductsException(String message) {
        super(message);
    }
}
