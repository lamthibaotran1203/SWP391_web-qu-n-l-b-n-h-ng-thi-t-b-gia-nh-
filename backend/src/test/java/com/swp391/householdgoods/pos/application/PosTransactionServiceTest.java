package com.swp391.householdgoods.pos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import com.swp391.householdgoods.pos.api.PosPaymentMethod;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.ExistingTransaction;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.SaleLine;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.SaleSku;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PosTransactionServiceTest {
    @Mock
    private PosTransactionRepository repository;

    @Mock
    private SalesOrderRepository salesOrders;

    @InjectMocks
    private PosTransactionService service;

    @Test
    void processCreatesPaidCounterOrderAndDeductsStockOnce() {
        UUID key = UUID.randomUUID();
        SalesOrderDetails order = orderDetails();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(repository.findActiveStaffId("staff@example.invalid")).thenReturn(Optional.of(4L));
        when(repository.lockSellableSkus(List.of(71L))).thenReturn(List.of(
                new SaleSku(71, "Ấm siêu tốc", "1.5 lít", "AM-001",
                        new BigDecimal("350000"), 5)));
        when(repository.createOrder(eq(key), startsWith("POS-"), eq(new BigDecimal("700000")),
                eq("staff@example.invalid"))).thenReturn(Optional.of(88L));
        when(repository.deductStock(88, key, 71, 2, "staff@example.invalid")).thenReturn(true);
        when(salesOrders.findDetails(88L)).thenReturn(Optional.of(order));

        SalesOrderDetails result = service.process(
                "staff@example.invalid",
                key,
                List.of(new SaleLine(71, 2)),
                PosPaymentMethod.CASH);

        assertThat(result).isEqualTo(order);
        verify(repository).deductStock(88, key, 71, 2, "staff@example.invalid");
        verify(repository).addOrderItem(88, new SaleSku(71, "Ấm siêu tốc", "1.5 lít",
                "AM-001", new BigDecimal("350000"), 5), 2, "staff@example.invalid");
        verify(repository).addPayment(88, key, new BigDecimal("700000"),
                PosPaymentMethod.CASH, "staff@example.invalid");
        verify(repository).addCompletedEvent(88, 4, key);
    }

    @Test
    void retryWithSameKeyReturnsSavedOrderWithoutDeductingAgain() {
        UUID key = UUID.randomUUID();
        SalesOrderDetails order = orderDetails();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.of(
                new ExistingTransaction(88, "CASH", List.of(new SaleLine(71, 2)))));
        when(salesOrders.findDetails(88)).thenReturn(Optional.of(order));

        assertThat(service.process(
                "staff@example.invalid", key, List.of(new SaleLine(71, 2)), PosPaymentMethod.CASH))
                .isEqualTo(order);

        verify(repository).findBySubmissionKey(key);
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
        verify(salesOrders).findDetails(88);
        org.mockito.Mockito.verifyNoMoreInteractions(salesOrders);
    }

    @Test
    void reusingSubmissionKeyForAnotherSaleIsRejected() {
        UUID key = UUID.randomUUID();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.of(
                new ExistingTransaction(88, "CASH", List.of(new SaleLine(71, 2)))));

        assertThatThrownBy(() -> service.process(
                "staff@example.invalid", key, List.of(new SaleLine(71, 1)), PosPaymentMethod.CASH))
                .isInstanceOf(PosTransactionException.class)
                .hasMessageContaining("nội dung bán hàng khác");

        verify(repository).findBySubmissionKey(key);
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
        verifyNoInteractions(salesOrders);
    }

    @Test
    void insufficientStockDoesNotCreateOrderOrChangeInventory() {
        UUID key = UUID.randomUUID();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.empty());
        when(repository.findActiveStaffId("staff@example.invalid")).thenReturn(Optional.of(4L));
        when(repository.lockSellableSkus(List.of(71L))).thenReturn(List.of(
                new SaleSku(71, "Ấm siêu tốc", "1.5 lít", "AM-001",
                        new BigDecimal("350000"), 1)));

        assertThatThrownBy(() -> service.process(
                "staff@example.invalid", key, List.of(new SaleLine(71, 2)), PosPaymentMethod.CASH))
                .isInstanceOf(PosTransactionException.class)
                .satisfies(error -> assertThat(((PosTransactionException) error).status())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void amountBeyondDatabaseLimitIsRejectedBeforeCreatingTheOrder() {
        UUID key = UUID.randomUUID();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(repository.findActiveStaffId("staff@example.invalid")).thenReturn(Optional.of(4L));
        when(repository.lockSellableSkus(List.of(71L))).thenReturn(List.of(
                new SaleSku(71, "Ấm siêu tốc", "1.5 lít", "AM-001",
                        new BigDecimal("999999999999999999"), 5)));

        assertThatThrownBy(() -> service.process(
                "staff@example.invalid", key, List.of(new SaleLine(71, 2)), PosPaymentMethod.CASH))
                .isInstanceOf(PosTransactionException.class)
                .hasMessageContaining("vượt quá giới hạn");
        verify(repository, times(2)).findBySubmissionKey(key);
        verify(repository).findActiveStaffId("staff@example.invalid");
        verify(repository).lockSellableSkus(List.of(71L));
        verify(repository, never()).createOrder(any(), anyString(), any(), anyString());
        verifyNoInteractions(salesOrders);
    }

    @Test
    void duplicateSkuLinesAreRejectedBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.process(
                "staff@example.invalid",
                UUID.randomUUID(),
                List.of(new SaleLine(71, 1), new SaleLine(71, 2)),
                PosPaymentMethod.CASH))
                .isInstanceOf(PosTransactionException.class)
                .hasMessageContaining("một lần");

        verifyNoInteractions(repository, salesOrders);
    }

    @Test
    void inactiveStaffCannotProcessASale() {
        UUID key = UUID.randomUUID();
        when(repository.findBySubmissionKey(key)).thenReturn(Optional.empty());
        when(repository.findActiveStaffId("inactive@example.invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(
                "inactive@example.invalid", key, List.of(new SaleLine(71, 1)), PosPaymentMethod.CASH))
                .isInstanceOf(PosTransactionException.class)
                .satisfies(error -> assertThat(((PosTransactionException) error).status())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(repository).findBySubmissionKey(key);
        verify(repository).findActiveStaffId("inactive@example.invalid");
        org.mockito.Mockito.verifyNoMoreInteractions(repository);
        verifyNoInteractions(salesOrders);
    }

    private SalesOrderDetails orderDetails() {
        return new SalesOrderDetails(
                88L, "POS-a-generated-code", "COUNTER", "COMPLETED", "PAID", "CASH",
                "Khách tại quầy", null, null, null,
                new BigDecimal("700000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("700000"), Instant.parse("2026-10-19T00:00:00Z"), List.of());
    }

}
