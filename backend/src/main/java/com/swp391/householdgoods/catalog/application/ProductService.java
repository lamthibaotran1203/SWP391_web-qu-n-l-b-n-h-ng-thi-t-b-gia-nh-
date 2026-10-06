package com.swp391.householdgoods.catalog.application;

import com.swp391.householdgoods.catalog.application.dto.CreateProductRequest;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.catalog.domain.Product;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.infrastructure.ProductRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findPublic(String search, String brand, String category,
                                            java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice,
                                            Pageable pageable) {
        String normalized = normalizeSearch(search);
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new IllegalArgumentException("Khoảng giá không hợp lệ.");
        }
        Specification<Product> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("status"), ProductStatus.ACTIVE));
            if (!normalized.isBlank()) {
                String pattern = "%" + normalized.toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("brand")), pattern),
                        builder.like(builder.lower(root.get("sku")), pattern)));
            }
            addEqualsFilter(builder, root, predicates, "brand", brand);
            addEqualsFilter(builder, root, predicates, "category", category);
            if (minPrice != null) predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Product> products = repository.findAll(specification, pageable);
        if (products.getTotalPages() > 0 && pageable.getPageNumber() >= products.getTotalPages()) {
            products = repository.findAll(specification,
                    org.springframework.data.domain.PageRequest.of(products.getTotalPages() - 1,
                            pageable.getPageSize(), pageable.getSort()));
        }
        return products.map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(repository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ProductNotFoundException(id)));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        repository.findBySkuIgnoreCase(request.sku().trim()).ifPresent(existing -> {
            throw new IllegalArgumentException("SKU đã tồn tại.");
        });
        Product product = new Product(request.sku().trim(), request.name().trim(), normalize(request.brand()), normalize(request.category()),
                request.price(), request.stockQuantity(), request.description(), request.imageUrl(), Instant.now());
        if (request.status() == ProductStatus.INACTIVE) {
            product.update(product.getName(), product.getBrand(), product.getCategory(), product.getPrice(), product.getStockQuantity(),
                    product.getDescription(), product.getImageUrl(), ProductStatus.INACTIVE, Instant.now());
        }
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, CreateProductRequest request) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        repository.findBySkuIgnoreCase(request.sku().trim()).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new IllegalArgumentException("SKU đã tồn tại.");
        });
        product.update(request.name().trim(), normalize(request.brand()), normalize(request.category()), request.price(), request.stockQuantity(),
                request.description(), request.imageUrl(), request.status() == null ? ProductStatus.ACTIVE : request.status(), Instant.now());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.update(product.getName(), product.getBrand(), product.getCategory(), product.getPrice(), product.getStockQuantity(),
                product.getDescription(), product.getImageUrl(), ProductStatus.INACTIVE, Instant.now());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSearch(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        return normalized;
    }

    private void addEqualsFilter(jakarta.persistence.criteria.CriteriaBuilder builder,
                                 jakarta.persistence.criteria.Root<Product> root,
                                 java.util.List<jakarta.persistence.criteria.Predicate> predicates,
                                 String field, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            predicates.add(builder.equal(builder.lower(root.get(field)), normalized.toLowerCase(java.util.Locale.ROOT)));
        }
    }
}
