package com.swp391.householdgoods.security;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
