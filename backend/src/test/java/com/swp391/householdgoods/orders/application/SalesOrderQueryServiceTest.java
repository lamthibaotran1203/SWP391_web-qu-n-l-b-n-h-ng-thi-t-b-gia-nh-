package com.swp391.householdgoods.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.orders.domain.SalesOrder;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SalesOrderQueryServiceTest {

    @Mock
    private SalesOrderRepository repository;

    @InjectMocks
    private SalesOrderQueryService service;

    @Test
    void searchReturnsPaginationMetadataAndSortsNewestFirst() {
        when(repository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 21));

        SalesOrderPage<SalesOrderSummary> result = service.search(
                new SalesOrderSearch(null, null, null, null, null, null),
                1,
                10);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(
                org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(),
                pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("placedAt").isDescending()).isTrue();
        assertThat(pageable.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
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
    void searchPassesThroughRequestedPageAndSize() {
        when(repository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 15), 0));

        service.search(new SalesOrderSearch(null, null, null, null, null, null), 2, 15);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(
                org.mockito.ArgumentMatchers.<Specification<SalesOrder>>any(),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(15);
    }
}
