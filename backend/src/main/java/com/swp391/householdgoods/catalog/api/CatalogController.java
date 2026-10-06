package com.swp391.householdgoods.catalog.api;

import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogQueryService service;

    public CatalogController(CatalogQueryService service) {
        this.service = service;
    }

    @GetMapping("/skus")
    public ApiEnvelope<CatalogPage> page(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new ApiEnvelope<>(service.page(
                search, brand, category, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/skus/{id}")
    public ApiEnvelope<CatalogSku> sku(@PathVariable long id) {
        return new ApiEnvelope<>(service.sku(id));
    }

    @GetMapping("/products/{id}")
    public ApiEnvelope<List<CatalogSku>> product(@PathVariable long id) {
        return new ApiEnvelope<>(service.product(id));
    }

    @GetMapping("/categories")
    public ApiEnvelope<List<CatalogReference>> categories() {
        return new ApiEnvelope<>(service.categories());
    }

    @GetMapping("/brands")
    public ApiEnvelope<List<CatalogReference>> brands() {
        return new ApiEnvelope<>(service.brands());
    }
}
