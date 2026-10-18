package com.swp391.householdgoods.catalog.category.api;

import com.swp391.householdgoods.catalog.category.application.ProductCategoryService;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryOption;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryRequest;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryResponse;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

@RestController
@RequestMapping("/api/v1/admin/categories")
@Validated
public class ProductCategoryController {

    private final ProductCategoryService service;

    public ProductCategoryController(ProductCategoryService service) {
        this.service = service;
    }

    @GetMapping
    ApiEnvelope<Page<CategoryResponse>> search(
            @RequestParam(required = false) @Size(max = 120) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return new ApiEnvelope<>(service.search(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    ApiEnvelope<CategoryResponse> get(@PathVariable @Positive Long id) {
        return new ApiEnvelope<>(service.get(id));
    }

    @GetMapping("/options")
    ApiEnvelope<List<CategoryOption>> options(
            @RequestParam(required = false) @Positive Long excludeId) {
        return new ApiEnvelope<>(service.parentOptions(excludeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<CategoryResponse> create(@Valid @RequestBody CategoryRequest request, Principal principal) {
        return new ApiEnvelope<>(service.create(request, actor(principal)));
    }

    @PutMapping("/{id}")
    ApiEnvelope<CategoryResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CategoryRequest request,
            Principal principal) {
        return new ApiEnvelope<>(service.update(id, request, actor(principal)));
    }

    @DeleteMapping("/{id}")
    ApiEnvelope<CategoryResponse> deactivate(@PathVariable @Positive Long id, Principal principal) {
        return new ApiEnvelope<>(service.deactivate(id, actor(principal)));
    }

    private String actor(Principal principal) {
        return principal == null ? "ADMIN" : principal.getName();
    }
}
