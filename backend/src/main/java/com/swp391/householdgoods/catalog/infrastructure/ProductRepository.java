package com.swp391.householdgoods.catalog.infrastructure;

import com.swp391.householdgoods.catalog.domain.Product;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);
    List<Product> findByStatus(ProductStatus status);
    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);
}
