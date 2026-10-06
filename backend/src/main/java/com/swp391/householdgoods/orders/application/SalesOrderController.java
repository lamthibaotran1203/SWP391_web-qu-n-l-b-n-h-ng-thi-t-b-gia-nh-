package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.orders.domain.OrderChannel;
import com.swp391.householdgoods.orders.domain.OrderStatus;
import com.swp391.householdgoods.orders.domain.PaymentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff/orders")
@Validated
public class SalesOrderController {

    private final SalesOrderQueryService queryService;

    public SalesOrderController(SalesOrderQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    ApiEnvelope<SalesOrderPage<SalesOrderSummary>> search(
            @RequestParam(required = false) @Size(max = 120) String q,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        SalesOrderSearch search = new SalesOrderSearch(q, channel, status, paymentStatus, from, to);
        return new ApiEnvelope<>(queryService.search(search, page, size));
    }

    @GetMapping("/{id}")
    ApiEnvelope<SalesOrderDetails> getDetails(@PathVariable @Min(1) long id) {
        return new ApiEnvelope<>(queryService.getDetails(id));
    }
}
