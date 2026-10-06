package com.swp391.householdgoods.purchasing.infrastructure;

import com.swp391.householdgoods.purchasing.domain.PurchaseOrder;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByIdAndIsDeletedFalse(Long id);

    Optional<PurchaseOrder> findByPoCode(String poCode);

    boolean existsByPoCode(String poCode);

    @Query("""
        SELECT po FROM PurchaseOrder po
        JOIN FETCH po.supplier s
        WHERE po.isDeleted = false
          AND (:supplierId IS NULL OR s.id = :supplierId)
          AND (:status IS NULL OR po.status = :status)
          AND (
            :keyword IS NULL
            OR LOWER(po.poCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY po.id DESC
    """)
    Page<PurchaseOrder> searchOrders(
            @Param("keyword") String keyword,
            @Param("supplierId") Long supplierId,
            @Param("status") PurchaseOrderStatus status,
            Pageable pageable);
}
