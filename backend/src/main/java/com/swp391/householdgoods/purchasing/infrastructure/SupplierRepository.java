package com.swp391.householdgoods.purchasing.infrastructure;

import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByIdAndIsDeletedFalse(Long id);

    Optional<Supplier> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByTaxCodeAndIsDeletedFalse(String taxCode);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Long id);

    boolean existsByTaxCodeAndIdNotAndIsDeletedFalse(String taxCode, Long id);

    List<Supplier> findAllByStatusAndIsDeletedFalseOrderByNameAsc(SupplierStatus status);

    @Query("""
        SELECT s FROM Supplier s
        WHERE s.isDeleted = false
          AND (:status IS NULL OR s.status = :status)
          AND (
            :keyword IS NULL
            OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.taxCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY s.id DESC
    """)
    Page<Supplier> searchSuppliers(
            @Param("keyword") String keyword,
            @Param("status") SupplierStatus status,
            Pageable pageable);
}
