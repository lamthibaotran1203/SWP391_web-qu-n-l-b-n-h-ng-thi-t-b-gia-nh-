package com.swp391.householdgoods.catalog.web;

import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final CatalogQueryService catalog;

    public ProductController(CatalogQueryService catalog) {
        this.catalog = catalog;
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
        var catalogPage = catalog.page(search, brand, category, minPrice, maxPrice,
                sort, safePage, safeSize);
        var products = catalogPage.items().stream().map(ProductResponse::from).toList();
        return new ApiEnvelope<>(new PageImpl<>(products,
                PageRequest.of(catalogPage.page(), catalogPage.size(), sortOf(sort)),
                catalogPage.totalElements()));
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
        return new ApiEnvelope<>(ProductResponse.from(catalog.sku(id)));
    }

    // Retired writes used immutable legacy_* fields. Public GET compatibility remains unchanged.
    @RequestMapping(method = {org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE})
    void retiredCollectionWrite() {
        throw retiredWrite();
    }

    @RequestMapping(value = "/{id}", method = {org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.DELETE})
    void retiredItemWrite() {
        throw retiredWrite();
    }

    private com.swp391.householdgoods.catalog.application.CatalogException retiredWrite() {
        return new com.swp391.householdgoods.catalog.application.CatalogException(HttpStatus.GONE,
                "CATALOG_WRITE_MOVED", "Dùng API quản lý /api/v1/manager/catalog/products với productId và SKU riêng.");
    }
}
