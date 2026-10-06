package com.swp391.householdgoods.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "sales_order_item", schema = "app")
public class SalesOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder order;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 64)
    private String productSku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    protected SalesOrderItem() {
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
