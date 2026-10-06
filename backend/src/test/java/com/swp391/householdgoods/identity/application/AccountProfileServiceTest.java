package com.swp391.householdgoods.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class AccountProfileServiceTest {

    private static final String EMAIL = "manager@example.invalid";
    private static final Instant NOW = Instant.parse("2026-10-06T07:00:00Z");

    @Mock
    private UserAccountRepository repository;

    private AccountProfileService service;

    @BeforeEach
    void setUp() {
        service = new AccountProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void updatesOnlyTheDisplayNameForTheAuthenticatedAccount() {
        UserAccount account = UserAccount.bootstrapManager(
                EMAIL,
                "existing-password-hash",
                "Old Name",
                NOW.minusSeconds(3600));
        when(repository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account));

        AuthenticatedAccount result = service.updateFullName("  MANAGER@example.invalid ", "  Bảo   Trân ");

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.fullName()).isEqualTo("Bảo Trân");
        assertThat(result.passwordHash()).isEqualTo("existing-password-hash");
        assertThat(account.getFullName()).isEqualTo("Bảo Trân");
        verify(repository).findByEmailIgnoreCase(EMAIL);
    }
}
