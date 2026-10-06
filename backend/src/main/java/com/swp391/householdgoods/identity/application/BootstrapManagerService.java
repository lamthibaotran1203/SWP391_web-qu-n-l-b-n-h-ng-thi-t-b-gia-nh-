package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.config.BootstrapManagerProperties;
import com.swp391.householdgoods.identity.domain.AccountRole;
import com.swp391.householdgoods.identity.domain.AccountStatus;
import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapManagerService {

    private static final Pattern PASSWORD_HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_HAS_DIGIT = Pattern.compile(".*[0-9].*");

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public BootstrapManagerService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public BootstrapManagerResult initialize(BootstrapManagerProperties properties) {
        String email = normalizeEmail(properties.email());
        String password = properties.password();
        String fullName = normalizeFullName(properties.fullName());

        validate(email, password, fullName);

        return repository.findByEmailIgnoreCase(email)
                .map(existing -> updateExisting(existing, email, fullName, properties.updateExisting()))
                .orElseGet(() -> createManager(email, password, fullName));
    }

    private BootstrapManagerResult createManager(String email, String password, String fullName) {
        Instant now = clock.instant();
        repository.save(UserAccount.bootstrapManager(
                email,
                passwordEncoder.encode(password),
                fullName,
                now));
        return BootstrapManagerResult.CREATED;
    }

    private BootstrapManagerResult updateExisting(
            UserAccount existing,
            String email,
            String fullName,
            boolean updateExisting) {
        if (existing.getRole() != AccountRole.MANAGER || existing.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "BOOTSTRAP_MANAGER_EMAIL belongs to a non-active Manager account: " + email);
        }
        if (!updateExisting) {
            return BootstrapManagerResult.UNCHANGED;
        }
        return existing.updateFullName(fullName, clock.instant())
                ? BootstrapManagerResult.UPDATED
                : BootstrapManagerResult.UNCHANGED;
    }

    private void validate(String email, String password, String fullName) {
        if (email.isBlank() || email.length() > 254 || !email.contains("@")) {
            throw new IllegalStateException("BOOTSTRAP_MANAGER_EMAIL must be a valid email up to 254 characters");
        }
        if (password == null
                || password.length() < 8
                || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !PASSWORD_HAS_LETTER.matcher(password).matches()
                || !PASSWORD_HAS_DIGIT.matcher(password).matches()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_MANAGER_PASSWORD must be 8-64 characters with at least one letter and digit, and at most 72 UTF-8 bytes");
        }
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new IllegalStateException("BOOTSTRAP_MANAGER_FULL_NAME must be 2-100 characters");
        }
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeFullName(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }
}
