package com.swp391.householdgoods.catalog.brand.api;

import com.swp391.householdgoods.catalog.brand.application.BrandService;
import com.swp391.householdgoods.catalog.brand.application.dto.BrandResponse;
import com.swp391.householdgoods.catalog.brand.application.dto.CreateBrandRequest;
import com.swp391.householdgoods.catalog.brand.application.dto.UpdateBrandRequest;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/brands")
@Validated
public class BrandManagementController {

    private final BrandService brandService;

    public BrandManagementController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ApiEnvelope<Page<BrandResponse>> list(
            @RequestParam(required = false) @Size(max = 120, message = "Từ khóa không quá 120 ký tự") String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Số trang không được nhỏ hơn 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Kích thước trang tối thiểu là 1") @Max(value = 100, message = "Kích thước trang tối đa là 100") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return new ApiEnvelope<>(brandService.searchBrands(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<BrandResponse> getById(@PathVariable @Min(value = 1, message = "ID không hợp lệ") Long id) {
        return new ApiEnvelope<>(brandService.getBrandById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<BrandResponse> create(
            @Valid @RequestBody CreateBrandRequest request,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "MANAGER";
        return new ApiEnvelope<>(brandService.createBrand(request, actor));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<BrandResponse> update(
            @PathVariable @Min(value = 1, message = "ID không hợp lệ") Long id,
            @Valid @RequestBody UpdateBrandRequest request,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "MANAGER";
        return new ApiEnvelope<>(brandService.updateBrand(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<BrandResponse> deactivate(
            @PathVariable @Min(value = 1, message = "ID không hợp lệ") Long id,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "MANAGER";
        return new ApiEnvelope<>(brandService.deactivateBrand(id, actor));
    }

    @PatchMapping("/{id}/status")
    public ApiEnvelope<BrandResponse> patchStatus(
            @PathVariable @Min(value = 1, message = "ID không hợp lệ") Long id,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "MANAGER";
        return new ApiEnvelope<>(brandService.deactivateBrand(id, actor));
    }
}
