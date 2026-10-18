package com.swp391.householdgoods.catalog.category.infrastructure;

import com.swp391.householdgoods.catalog.category.domain.ProductCategory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    @Query("""
            select c from ProductCategory c
            where (:keyword = '' or lower(c.code) like lower(concat('%', :keyword, '%'))
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.description, '')) like lower(concat('%', :keyword, '%')))
              and (:deleted is null or c.deleted = :deleted)
            """)
    Page<ProductCategory> search(
            @Param("keyword") String keyword,
            @Param("deleted") Boolean deleted,
            Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    long countByParentIdAndDeletedFalse(Long parentId);
    List<ProductCategory> findAllByDeletedFalseOrderByNameAsc();

    @Query(value = "select count(*) from app.product where category_id=:categoryId and status='ACTIVE' and not is_deleted",
            nativeQuery = true)
    long countActiveProducts(@Param("categoryId") Long categoryId);
}
