package com.swp391.householdgoods.pos.api;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.common.api.ApiProblem;
import com.swp391.householdgoods.common.web.TraceIdFilter;
import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.pos.application.PosTransactionException;
import com.swp391.householdgoods.pos.application.PosTransactionService;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.SaleLine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/pos/transactions")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class PosTransactionController {
    private final PosTransactionService service;

    public PosTransactionController(PosTransactionService service) {
        this.service = service;
    }

    public record ItemRequest(
            @NotNull @Positive Long skuId,
            @NotNull @Positive Integer quantity) {}

    public record ProcessRequest(
            @NotEmpty @Size(max = 50) List<@NotNull @Valid ItemRequest> items,
            @NotNull PosPaymentMethod paymentMethod) {}

    @PostMapping
    public ApiEnvelope<SalesOrderDetails> process(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody ProcessRequest request) {
        List<SaleLine> items = request.items().stream()
                .map(item -> new SaleLine(item.skuId(), item.quantity()))
                .toList();
        return new ApiEnvelope<>(service.process(
                authentication.getName(), idempotencyKey, items, request.paymentMethod()));
    }

    @ExceptionHandler(PosTransactionException.class)
    public ResponseEntity<ApiProblem> handlePosTransaction(
            PosTransactionException exception,
            HttpServletRequest request) {
        int status = exception.status().value();
        ApiProblem problem = ApiProblem.of(
                status,
                exception.status().getReasonPhrase(),
                exception.code(),
                exception.getMessage(),
                Map.of(),
                TraceIdFilter.getTraceId(request));
        return ResponseEntity.status(status).body(problem);
    }
}
