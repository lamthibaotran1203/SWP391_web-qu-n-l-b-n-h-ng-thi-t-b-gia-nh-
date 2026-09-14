package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountQueryService {

    private final UserAccountRepository repository;

    public AccountQueryService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AuthenticatedAccount findForAuthentication(String email) {
        UserAccount account = repository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return new AuthenticatedAccount(
                account.getEmail(),
                account.getPasswordHash(),
                account.getFullName(),
                account.getRole(),
                account.getStatus());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
