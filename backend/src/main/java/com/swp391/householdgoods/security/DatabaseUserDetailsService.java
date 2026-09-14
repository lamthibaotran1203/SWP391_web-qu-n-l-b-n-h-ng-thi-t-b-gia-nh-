package com.swp391.householdgoods.security;

import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.identity.application.AuthenticatedAccount;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountQueryService accountQueryService;

    public DatabaseUserDetailsService(AccountQueryService accountQueryService) {
        this.accountQueryService = accountQueryService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AuthenticatedAccount account = accountQueryService.findForAuthentication(username);
        return User.withUsername(account.email())
                .password(account.passwordHash() == null ? "" : account.passwordHash())
                .roles(account.role().name())
                .disabled(account.status() != AccountStatus.ACTIVE)
                .build();
    }
}
