package com.swp391.householdgoods.purchasing.api;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.purchasing.application.PurchaseOrderService;
import com.swp391.householdgoods.purchasing.application.dto.CreatePurchaseOrderRequest;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderResponse;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderStatus;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchasing/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<PurchaseOrderResponse> create(
            @Valid @RequestBody CreatePurchaseOrderRequest request,
            Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "ADMIN";
        return new ApiEnvelope<>(purchaseOrderService.createPurchaseOrder(request, actor));
    }

    @GetMapping
    public ApiEnvelope<Page<PurchaseOrderResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        return new ApiEnvelope<>(purchaseOrderService.searchPurchaseOrders(keyword, supplierId, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiEnvelope<PurchaseOrderResponse> getById(@PathVariable Long id) {
        return new ApiEnvelope<>(purchaseOrderService.getPurchaseOrderById(id));
    }
}
