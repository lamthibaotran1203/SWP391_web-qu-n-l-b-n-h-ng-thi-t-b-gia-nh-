package com.swp391.householdgoods.catalog.web;

import com.swp391.householdgoods.catalog.application.ProductService;
import com.swp391.householdgoods.catalog.application.dto.CreateProductRequest;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    ApiEnvelope<Page<ProductResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (brand != null && brand.trim().length() > 100 || category != null && category.trim().length() > 100) {
            throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        }
        return new ApiEnvelope<>(service.findPublic(search, brand, category, minPrice, maxPrice,
                PageRequest.of(safePage, safeSize, sortOf(sort))));
    }

    private Sort sortOf(String value) {
        return switch (value == null ? "newest" : value.trim().toLowerCase()) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> throw new IllegalArgumentException("Sort không được hỗ trợ.");
        };
    }

    @GetMapping("/{id}")
    ApiEnvelope<ProductResponse> get(@PathVariable Long id) {
        return new ApiEnvelope<>(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return new ApiEnvelope<>(service.create(request));
    }

    @PutMapping("/{id}")
    ApiEnvelope<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody CreateProductRequest request) {
        return new ApiEnvelope<>(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
