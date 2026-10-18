package com.swp391.householdgoods.catalog.management.api;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.management.application.CatalogManagementService;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/admin/catalog/products")
public class CatalogManagementController {
    private final CatalogManagementService service;

    public CatalogManagementController(CatalogManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiEnvelope<ManagementPage> page(@RequestParam(required = false) String search,
            @RequestParam(required = false) ProductStatus status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return safe(() -> service.page(search, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<ManagedProduct> detail(@PathVariable long id) {
        return safe(() -> service.detail(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<ManagedProduct> create(@Valid @RequestBody CatalogWriteRequest.CreateProduct body, Authentication auth) {
        return safe(() -> service.create(body, auth.getName()));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<ManagedProduct> edit(@PathVariable long id, @Valid @RequestBody CatalogWriteRequest.EditProduct body) {
        return safe(() -> service.editProduct(id, body));
    }

    @PostMapping("/{id}/skus")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<ManagedProduct> addSku(@PathVariable long id, @Valid @RequestBody CatalogWriteRequest.AddSku body, Authentication auth) {
        return safe(() -> service.addSku(id, body, auth.getName()));
    }

    @PutMapping("/{id}/skus/{skuId}")
    public ApiEnvelope<ManagedProduct> editSku(@PathVariable long id, @PathVariable long skuId,
            @Valid @RequestBody CatalogWriteRequest.EditSku body, Authentication auth) {
        return safe(() -> service.editSku(id, skuId, body, auth.getName()));
    }

    private <T> ApiEnvelope<T> safe(Supplier<T> operation) {
        try {
            return new ApiEnvelope<>(operation.get());
        } catch (DataAccessException | TransactionException unavailable) {
            throw new CatalogException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_SAVE_UNAVAILABLE",
                    "Chưa thể tải hoặc lưu sản phẩm. Vui lòng thử lại.");
        }
    }
}
