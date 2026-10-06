package com.swp391.householdgoods.purchasing.infrastructure;

import com.swp391.householdgoods.purchasing.domain.PurchaseOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseOrderId(Long poId);
}
