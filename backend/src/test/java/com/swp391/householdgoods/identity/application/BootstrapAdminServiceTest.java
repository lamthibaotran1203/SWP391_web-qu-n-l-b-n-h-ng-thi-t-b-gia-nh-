package com.swp391.householdgoods.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.config.BootstrapAdminProperties;
import com.swp391.householdgoods.identity.domain.UserAccount;
import com.swp391.householdgoods.identity.infrastructure.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminServiceTest {

    private static final String EMAIL = "admin@example.invalid";
    private static final String PASSWORD = "Admin123";
    private static final Instant NOW = Instant.parse("2026-10-06T07:00:00Z");

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private BootstrapAdminService service;

    @BeforeEach
    void setUp() {
        service = new BootstrapAdminService(
                repository,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void existingAdminNameIsUnchangedByDefault() {
        UserAccount admin = admin("Existing Name");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(admin));

        BootstrapAdminResult result = service.initialize(properties("New Name", false));

        assertThat(result).isEqualTo(BootstrapAdminResult.UNCHANGED);
        assertThat(admin.getFullName()).isEqualTo("Existing Name");
        verify(passwordEncoder, never()).encode(PASSWORD);
    }

    @Test
    void optInUpdatesOnlyExistingAdminDisplayName() {
        UserAccount admin = admin("Existing Name");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(admin));

        BootstrapAdminResult result = service.initialize(properties("  Bảo   Trân  ", true));

        assertThat(result).isEqualTo(BootstrapAdminResult.UPDATED);
        assertThat(admin.getFullName()).isEqualTo("Bảo Trân");
        verify(passwordEncoder, never()).encode(PASSWORD);
    }

    @Test
    void optInDoesNotReportAnUpdateWhenNameIsAlreadyCurrent() {
        UserAccount admin = admin("Bảo Trân");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(admin));

        BootstrapAdminResult result = service.initialize(properties("Bảo Trân", true));

        assertThat(result).isEqualTo(BootstrapAdminResult.UNCHANGED);
    }

    private UserAccount admin(String fullName) {
        return UserAccount.bootstrapAdmin(
                EMAIL,
                "existing-password-hash",
                fullName,
                NOW.minusSeconds(3600));
    }

    private BootstrapAdminProperties properties(String fullName, boolean updateExisting) {
        return new BootstrapAdminProperties(EMAIL, PASSWORD, fullName, updateExisting);
    }
}
