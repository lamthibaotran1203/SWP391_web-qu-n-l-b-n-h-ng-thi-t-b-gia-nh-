package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileService {

    private final UserAccountRepository repository;
    private final Clock clock;

    public AccountProfileService(UserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public AuthenticatedAccount updateFullName(String email, String requestedName) {
        String normalizedEmail = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        String fullName = requestedName.strip().replaceAll("\\s+", " ");
        UserAccount account = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        account.updateFullName(fullName, clock.instant());
        return new AuthenticatedAccount(
                account.getEmail(),
                account.getPasswordHash(),
                account.getFullName(),
                account.getRole(),
                account.getStatus());
    }
}
