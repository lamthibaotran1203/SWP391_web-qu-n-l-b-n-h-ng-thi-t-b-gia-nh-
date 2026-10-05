package com.swp391.householdgoods.catalog.api;

import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogQueryService service;
    public CatalogController(CatalogQueryService service) { this.service=service; }
    @GetMapping("/skus")
    public ApiEnvelope<CatalogPage> page(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return new ApiEnvelope<>(service.page(page,size)); }
    @GetMapping("/skus/{id}")
    public ApiEnvelope<CatalogSku> sku(@PathVariable long id) { return new ApiEnvelope<>(service.sku(id)); }
    @GetMapping("/products/{id}")
    public ApiEnvelope<List<CatalogSku>> product(@PathVariable long id) { return new ApiEnvelope<>(service.product(id)); }
    @GetMapping("/categories")
    public ApiEnvelope<List<CatalogReference>> categories() { return new ApiEnvelope<>(service.categories()); }
    @GetMapping("/brands")
    public ApiEnvelope<List<CatalogReference>> brands() { return new ApiEnvelope<>(service.brands()); }
}
