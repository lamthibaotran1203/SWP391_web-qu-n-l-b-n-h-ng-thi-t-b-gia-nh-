package com.swp391.householdgoods.identity.application.dto;

/** Safe contact data only; credentials and account permissions are never returned. */
public record PersonalProfile(String email, String fullName, String phone) {}
