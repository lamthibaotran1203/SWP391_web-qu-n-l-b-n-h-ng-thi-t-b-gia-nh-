package com.swp391.householdgoods.security;

public record CurrentUserResponse(
        boolean authenticated,
        String email,
        String fullName,
        String role) {

    public static CurrentUserResponse anonymous() {
        return new CurrentUserResponse(false, null, null, null);
    }
}
