package com.swp391.householdgoods.pos.application;

import org.springframework.http.HttpStatus;

public class PosTransactionException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public PosTransactionException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
