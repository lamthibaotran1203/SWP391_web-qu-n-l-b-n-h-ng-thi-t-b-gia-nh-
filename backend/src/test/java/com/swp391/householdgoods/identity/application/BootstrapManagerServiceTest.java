package com.swp391.householdgoods.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.config.BootstrapManagerProperties;
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
class BootstrapManagerServiceTest {

    private static final String EMAIL = "manager@example.invalid";
    private static final String PASSWORD = "Manager123";
    private static final Instant NOW = Instant.parse("2026-10-06T07:00:00Z");

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private BootstrapManagerService service;

    @BeforeEach
    void setUp() {
        service = new BootstrapManagerService(
                repository,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void existingManagerNameIsUnchangedByDefault() {
        UserAccount manager = manager("Existing Name");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(manager));

        BootstrapManagerResult result = service.initialize(properties("New Name", false));

        assertThat(result).isEqualTo(BootstrapManagerResult.UNCHANGED);
        assertThat(manager.getFullName()).isEqualTo("Existing Name");
        verify(passwordEncoder, never()).encode(PASSWORD);
    }

    @Test
    void optInUpdatesOnlyExistingManagerDisplayName() {
        UserAccount manager = manager("Existing Name");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(manager));

        BootstrapManagerResult result = service.initialize(properties("  Bảo   Trân  ", true));

        assertThat(result).isEqualTo(BootstrapManagerResult.UPDATED);
        assertThat(manager.getFullName()).isEqualTo("Bảo Trân");
        verify(passwordEncoder, never()).encode(PASSWORD);
    }

    @Test
    void optInDoesNotReportAnUpdateWhenNameIsAlreadyCurrent() {
        UserAccount manager = manager("Bảo Trân");
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(manager));

        BootstrapManagerResult result = service.initialize(properties("Bảo Trân", true));

        assertThat(result).isEqualTo(BootstrapManagerResult.UNCHANGED);
    }

    private UserAccount manager(String fullName) {
        return UserAccount.bootstrapManager(
                EMAIL,
                "existing-password-hash",
                fullName,
                NOW.minusSeconds(3600));
    }

    private BootstrapManagerProperties properties(String fullName, boolean updateExisting) {
        return new BootstrapManagerProperties(EMAIL, PASSWORD, fullName, updateExisting);
    }
}
