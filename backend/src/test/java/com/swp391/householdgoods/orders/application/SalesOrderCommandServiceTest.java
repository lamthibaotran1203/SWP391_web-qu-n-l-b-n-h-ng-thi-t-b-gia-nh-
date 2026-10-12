package com.swp391.householdgoods.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesOrderCommandServiceTest {

    @Mock
    private SalesOrderRepository repository;

    @InjectMocks
    private SalesOrderCommandService service;

    @Test
    void confirmReturnsUpdatedOrderDetails() {
        SalesOrderDetails expected = new SalesOrderDetails(
                41L,
                "HG-2026-0041",
                "ONLINE",
                "CONFIRMED",
                "PENDING",
                "COD",
                "Nguyễn Minh Anh",
                "minhanh@example.invalid",
                "0901234567",
                "12 Nguyễn Huệ",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.parse("2026-10-05T03:00:00Z"),
                List.of());
        when(repository.confirmIfPending(41)).thenReturn(1);
        when(repository.findDetails(41)).thenReturn(Optional.of(expected));

        assertThat(service.confirm(41)).isEqualTo(expected);

        verify(repository).confirmIfPending(41);
        verify(repository).findDetails(41);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void confirmRejectsAnExistingOrderWithAnotherStatus() {
        when(repository.confirmIfPending(41)).thenReturn(0);
        when(repository.findStatus(41)).thenReturn(Optional.of("PROCESSING"));

        assertThatThrownBy(() -> service.confirm(41))
                .isInstanceOf(SalesOrderStatusConflictException.class);

        verify(repository).confirmIfPending(41);
        verify(repository).findStatus(41);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void confirmRejectsAnUnknownOrder() {
        when(repository.confirmIfPending(404)).thenReturn(0);
        when(repository.findStatus(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(404))
                .isInstanceOf(SalesOrderNotFoundException.class);

        verify(repository).confirmIfPending(404);
        verify(repository).findStatus(404);
        verifyNoMoreInteractions(repository);
    }
}
