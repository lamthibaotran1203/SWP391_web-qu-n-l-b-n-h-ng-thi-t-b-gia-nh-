package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.config.BootstrapAdminProperties;
import com.swp391.householdgoods.identity.domain.AccountRole;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapAdminService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public BootstrapAdminService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public BootstrapAdminResult initialize(BootstrapAdminProperties properties) {
        String email = normalizeEmail(properties.email());
        String password = properties.password();
        String fullName = normalizeFullName(properties.fullName());

        validate(email, password, fullName);

        return repository.findByEmailIgnoreCase(email)
                .map(existing -> updateExisting(existing, email, fullName, properties.updateExisting()))
                .orElseGet(() -> createAdmin(email, password, fullName));
    }

    private BootstrapAdminResult createAdmin(String email, String password, String fullName) {
        Instant now = clock.instant();
        repository.save(UserAccount.bootstrapAdmin(
                email,
                passwordEncoder.encode(password),
                fullName,
                now));
        return BootstrapAdminResult.CREATED;
    }

    private BootstrapAdminResult updateExisting(
            UserAccount existing,
            String email,
            String fullName,
            boolean updateExisting) {
        if (existing.getRole() != AccountRole.ADMIN || existing.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_EMAIL belongs to a non-active Admin account: " + email);
        }
        if (!updateExisting) {
            return BootstrapAdminResult.UNCHANGED;
        }
        return existing.updateFullName(fullName, clock.instant())
                ? BootstrapAdminResult.UPDATED
                : BootstrapAdminResult.UNCHANGED;
    }

    private void validate(String email, String password, String fullName) {
        if (email.isBlank() || email.length() > 254 || !email.contains("@")) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL must be a valid email up to 254 characters");
        }
        if (!PasswordPolicy.accepts(password)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be 8-64 characters and at most 72 UTF-8 bytes");
        }
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_FULL_NAME must be 2-100 characters");
        }
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeFullName(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }
}
