package com.swp391.householdgoods.identity.application;

public record VerificationEmailRequested(
        String email,
        String fullName,
        String verificationUrl) {
}