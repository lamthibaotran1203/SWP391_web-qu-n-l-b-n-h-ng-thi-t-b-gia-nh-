package com.swp391.householdgoods.catalog.category.application;

import org.springframework.http.HttpStatus;

public class CategoryManagementException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CategoryManagementException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
