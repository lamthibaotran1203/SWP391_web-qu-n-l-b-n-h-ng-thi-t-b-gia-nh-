package com.swp391.householdgoods.catalog.brand.infrastructure;

import com.swp391.householdgoods.catalog.brand.domain.Brand;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Brand b WHERE LOWER(TRIM(b.name)) = LOWER(TRIM(:name)) AND b.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query("""
        SELECT b FROM Brand b
        WHERE (:isDeleted IS NULL OR b.isDeleted = :isDeleted)
          AND (
            :keyword = ''
            OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR (b.country IS NOT NULL AND LOWER(b.country) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        ORDER BY b.id DESC
    """)
    Page<Brand> searchBrands(
            @Param("keyword") String keyword,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM app.product WHERE brand_id = :brandId AND status = 'ACTIVE' AND NOT is_deleted", nativeQuery = true)
    long countActiveProductsByBrandId(@Param("brandId") Long brandId);
}
