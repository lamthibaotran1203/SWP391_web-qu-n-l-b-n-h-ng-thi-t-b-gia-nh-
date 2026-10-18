package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.common.api.ApiProblem;
import com.swp391.householdgoods.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    public record AddRequest(
            @NotNull @Positive Long skuId,
            @NotNull @Positive @Digits(integer = 10, fraction = 0)
            @DecimalMax("2147483647") BigDecimal quantity) {}
    public record QuantityRequest(
            @NotNull @Positive @Digits(integer = 10, fraction = 0)
            @DecimalMax("2147483647") BigDecimal quantity,
            @NotNull UUID expectedVersion) {}
    public record RemoveRequest(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> skuIds) {}

    @GetMapping
    public ApiEnvelope<CartService.Cart> view(Authentication authentication) {
        return new ApiEnvelope<>(service.view(authentication.getName()));
    }

    @PostMapping("/items")
    public ApiEnvelope<CartService.Cart> add(Authentication authentication,
            @RequestHeader("Idempotency-Key") UUID requestId,
            @Valid @RequestBody AddRequest request) {
        CartService.Cart cart = service.addItem(authentication.getName(), request.skuId(),
                request.quantity().intValueExact(), requestId);
        return new ApiEnvelope<>(cart);
    }

    @PutMapping("/items/{skuId}")
    public ApiEnvelope<CartService.Cart> update(Authentication authentication, @PathVariable long skuId,
            @Valid @RequestBody QuantityRequest request) {
        CartService.Cart cart = service.updateItem(authentication.getName(), skuId,
                request.quantity().intValueExact(), request.expectedVersion());
        return new ApiEnvelope<>(cart);
    }

    @DeleteMapping("/items")
    public ApiEnvelope<CartService.RemovalResult> remove(
            Authentication authentication, @Valid @RequestBody RemoveRequest request) {
        return new ApiEnvelope<>(service.remove(authentication.getName(), request.skuIds()));
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ApiProblem> databaseFailure(HttpServletRequest request) {
        ApiProblem problem = ApiProblem.of(503, "Service Unavailable", "CART_UNAVAILABLE",
                "Chưa thể tải hoặc lưu giỏ hàng. Vui lòng thử lại.", Map.of(),
                TraceIdFilter.getTraceId(request));
        return ResponseEntity.status(503).body(problem);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ApiProblem> missingRequestId(HttpServletRequest request) {
        ApiProblem problem = ApiProblem.of(400, "Bad Request", "CART_REQUEST_ID_REQUIRED",
                "Yêu cầu thêm giỏ hàng phải có mã chống gửi lặp.", Map.of(),
                TraceIdFilter.getTraceId(request));
        return ResponseEntity.badRequest().body(problem);
    }
}
