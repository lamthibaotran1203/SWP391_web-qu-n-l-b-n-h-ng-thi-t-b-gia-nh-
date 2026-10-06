package com.swp391.householdgoods.orders.infrastructure;

import com.swp391.householdgoods.orders.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {
}
