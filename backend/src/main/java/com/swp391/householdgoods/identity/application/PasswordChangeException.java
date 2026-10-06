package com.swp391.householdgoods.identity.application;

import org.springframework.http.HttpStatus;

public final class PasswordChangeException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String field;

    public PasswordChangeException(HttpStatus status, String code, String message, String field) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String field() { return field; }
}
