package com.swp391.householdgoods.purchasing.api;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.purchasing.application.SupplierService;
import com.swp391.householdgoods.purchasing.application.dto.CreateSupplierRequest;
import com.swp391.householdgoods.purchasing.application.dto.SupplierResponse;
import com.swp391.householdgoods.purchasing.application.dto.UpdateSupplierRequest;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/purchasing/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ApiEnvelope<Page<SupplierResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        return new ApiEnvelope<>(supplierService.searchSuppliers(keyword, status, pageable));
    }

    @GetMapping("/active")
    public ApiEnvelope<List<SupplierResponse>> getActiveSuppliers() {
        return new ApiEnvelope<>(supplierService.getActiveSuppliers());
    }

    @GetMapping("/{id}")
    public ApiEnvelope<SupplierResponse> getById(@PathVariable Long id) {
        return new ApiEnvelope<>(supplierService.getSupplierById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<SupplierResponse> create(
            @Valid @RequestBody CreateSupplierRequest request,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "ADMIN";
        return new ApiEnvelope<>(supplierService.createSupplier(request, actor));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "ADMIN";
        return new ApiEnvelope<>(supplierService.updateSupplier(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<String> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "ADMIN";
        supplierService.deleteSupplier(id, actor);
        return new ApiEnvelope<>("Đã vô hiệu hóa nhà cung cấp thành công.");
    }
}
