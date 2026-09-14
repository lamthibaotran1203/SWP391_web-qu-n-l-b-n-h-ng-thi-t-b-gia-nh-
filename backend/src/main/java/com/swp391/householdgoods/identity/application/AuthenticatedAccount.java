package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.domain.AccountRole;
import com.swp391.householdgoods.identity.domain.AccountStatus;

public record AuthenticatedAccount(
        String email,
        String passwordHash,
        String fullName,
        AccountRole role,
        AccountStatus status) {
}
