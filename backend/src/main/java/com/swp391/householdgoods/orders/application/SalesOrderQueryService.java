package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.api.SalesOrderPage;
import com.swp391.householdgoods.orders.api.SalesOrderSummary;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrderQueryService {

    private final SalesOrderRepository repository;

    public SalesOrderQueryService(SalesOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesOrderPage<SalesOrderSummary> search(
            SalesOrderSearch search,
            int page,
            int size) {
        if (search.from() != null && search.to() != null && search.from().isAfter(search.to())) {
            throw new InvalidSalesOrderSearchException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        return repository.search(search, page, size);
    }

    @Transactional(readOnly = true)
    public SalesOrderDetails getDetails(long id) {
        Optional<SalesOrderDetails> details = repository.findDetails(id);
        return details.orElseThrow(() -> new SalesOrderNotFoundException(id));
    }
}
