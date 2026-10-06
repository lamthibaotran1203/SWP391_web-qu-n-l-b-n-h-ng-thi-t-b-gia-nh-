package com.swp391.householdgoods.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order", schema = "app")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, length = 32)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<SalesOrderItem> items = new ArrayList<>();

    protected SalesOrder() {
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public List<SalesOrderItem> getItems() {
        return List.copyOf(items);
    }
}
