package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrderCommandService {

    private final SalesOrderRepository repository;

    public SalesOrderCommandService(SalesOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SalesOrderDetails confirm(long id) {
        if (repository.confirmIfPending(id) == 0) {
            Optional<String> status = repository.findStatus(id);
            if (status.isEmpty()) {
                throw new SalesOrderNotFoundException(id);
            }
            throw new SalesOrderStatusConflictException(id, status.get());
        }

        return repository.findDetails(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));
    }
}
