package com.swp391.householdgoods.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.orders.api.SalesOrderPage;
import com.swp391.householdgoods.orders.api.SalesOrderSummary;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesOrderQueryServiceTest {

    @Mock
    private SalesOrderRepository repository;

    @InjectMocks
    private SalesOrderQueryService service;

    @Test
    void searchReturnsRepositoryPage() {
        SalesOrderPage<SalesOrderSummary> expected = new SalesOrderPage<>(List.of(), 1, 10, 21, 3);
        SalesOrderSearch search = new SalesOrderSearch(null, null, null, null, null, null);
        when(repository.search(search, 1, 10)).thenReturn(expected);

        SalesOrderPage<SalesOrderSummary> result = service.search(search, 1, 10);

        assertThat(result).isEqualTo(expected);
        verify(repository).search(search, 1, 10);
    }

    @Test
    void searchRejectsAnInvertedDateRangeBeforeQuerying() {
        SalesOrderSearch invalidRange = new SalesOrderSearch(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> service.search(invalidRange, 0, 20))
                .isInstanceOf(InvalidSalesOrderSearchException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void detailsRaiseNotFoundForUnknownOrder() {
        when(repository.findDetails(404)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getDetails(404))
                .isInstanceOf(SalesOrderNotFoundException.class);
    }
}
