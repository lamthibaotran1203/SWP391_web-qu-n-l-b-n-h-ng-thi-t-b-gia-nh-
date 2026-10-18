package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @DecimalMax("2147483647") BigDecimal quantity) {}

    public record RemoveRequest(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> skuIds) {}

    @GetMapping
    public ApiEnvelope<CartService.Cart> view(Authentication authentication) {
        String email = authentication.getName();
        return executeCartOperation(() -> service.view(email));
    }

    @PostMapping("/items")
    public ApiEnvelope<CartService.Cart> add(
            Authentication authentication, @Valid @RequestBody AddRequest request) {
        String email = authentication.getName();
        int quantity = request.quantity().intValueExact();
        return executeCartOperation(() -> service.save(email, request.skuId(), quantity, true));
    }

    @PutMapping("/items/{skuId}")
    public ApiEnvelope<CartService.Cart> update(
            Authentication authentication, @PathVariable long skuId,
            @Valid @RequestBody QuantityRequest request) {
        String email = authentication.getName();
        int quantity = request.quantity().intValueExact();
        return executeCartOperation(() -> service.save(email, skuId, quantity, false));
    }

    @DeleteMapping("/items")
    public ApiEnvelope<CartService.Cart> remove(
            Authentication authentication, @Valid @RequestBody RemoveRequest request) {
        String email = authentication.getName();
        return executeCartOperation(() -> service.remove(email, request.skuIds()));
    }

    // operation là lời gọi Service. Dùng chung phần xử lý lỗi database cho bốn API.
    private ApiEnvelope<CartService.Cart> executeCartOperation(Supplier<CartService.Cart> operation) {
        try {
            CartService.Cart cart = operation.get();
            return new ApiEnvelope<>(cart);
        } catch (DataAccessException | TransactionException failure) {
            throw new CatalogException(HttpStatus.SERVICE_UNAVAILABLE, "CART_UNAVAILABLE",
                    "Chưa thể tải hoặc lưu giỏ hàng. Vui lòng thử lại.");
        }
    }
}
