package com.swp391.householdgoods.orders.application;

import com.swp391.householdgoods.orders.domain.SalesOrder;
import com.swp391.householdgoods.orders.domain.SalesOrderItem;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesOrderQueryService {

    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SalesOrderRepository repository;

    public SalesOrderQueryService(SalesOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SalesOrderPage<SalesOrderSummary> search(
            SalesOrderSearch search,
            int page,
            int size) {
        if (search.from() != null && search.to() != null && search.from().isAfter(search.to())) {
            throw new InvalidSalesOrderSearchException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        Page<SalesOrder> orders = repository.findAll(
                specification(search),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.desc("placedAt"), Sort.Order.desc("id"))));

        return new SalesOrderPage<>(
                orders.map(this::toSummary).getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SalesOrderDetails getDetails(long id) {
        SalesOrder order = repository.findById(id)
                .orElseThrow(() -> new SalesOrderNotFoundException(id));

        List<SalesOrderLineDetails> items = order.getItems().stream()
                .map(this::toLineDetails)
                .toList();
        return new SalesOrderDetails(
                order.getId(),
                order.getOrderNumber(),
                order.getChannel(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getShippingAddress(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                items);
    }

    private Specification<SalesOrder> specification(SalesOrderSearch search) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search.query() != null && !search.query().isBlank()) {
                String normalized = search.query().trim().toLowerCase(Locale.ROOT);
                String pattern = "%" + escapeLike(normalized) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("orderNumber")), pattern, '\\'),
                        builder.like(builder.lower(root.get("customerName")), pattern, '\\'),
                        builder.like(builder.lower(root.get("customerEmail")), pattern, '\\'),
                        builder.like(builder.lower(root.get("customerPhone")), pattern, '\\')));
            }
            if (search.channel() != null) {
                predicates.add(builder.equal(root.get("channel"), search.channel()));
            }
            if (search.status() != null) {
                predicates.add(builder.equal(root.get("status"), search.status()));
            }
            if (search.paymentStatus() != null) {
                predicates.add(builder.equal(root.get("paymentStatus"), search.paymentStatus()));
            }
            if (search.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("placedAt"),
                        search.from().atStartOfDay(BUSINESS_TIME_ZONE).toInstant()));
            }
            if (search.to() != null) {
                predicates.add(builder.lessThan(
                        root.get("placedAt"),
                        search.to().plusDays(1).atStartOfDay(BUSINESS_TIME_ZONE).toInstant()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private SalesOrderSummary toSummary(SalesOrder order) {
        return new SalesOrderSummary(
                order.getId(),
                order.getOrderNumber(),
                order.getChannel(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getTotalAmount(),
                order.getPlacedAt());
    }

    private SalesOrderLineDetails toLineDetails(SalesOrderItem item) {
        return new SalesOrderLineDetails(
                item.getId(),
                item.getProductName(),
                item.getProductSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }
}
