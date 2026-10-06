package com.swp391.householdgoods.purchasing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swp391.householdgoods.purchasing.application.dto.CreatePurchaseOrderRequest;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderItemRequest;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderResponse;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrder;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderStatus;
import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import com.swp391.householdgoods.purchasing.infrastructure.PurchaseOrderItemRepository;
import com.swp391.householdgoods.purchasing.infrastructure.PurchaseOrderRepository;
import com.swp391.householdgoods.purchasing.infrastructure.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier activeSupplier;

    @BeforeEach
    void setUp() {
        activeSupplier = new Supplier(
                "SUP-01",
                "0312345678",
                "Sunhouse",
                "Nguyen A",
                "0901234567",
                "a@sunhouse.vn",
                "Ha Noi",
                "admin"
        );
        activeSupplier.setId(1L);
        activeSupplier.setStatus(SupplierStatus.ACTIVE);
    }

    @Test
    void createPurchaseOrderSuccessfullyCalculatesVndTotalCost() {
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));

        // Mock SKU lookup for SKU 101 and 102
        when(jdbcTemplate.queryForList(any(String.class), eq(101L)))
                .thenReturn(List.of(Map.of("id", 101L, "sku_code", "SKU-NOI-COM", "sku_name", "Nồi cơm điện", "status", "ACTIVE", "is_deleted", false)));
        when(jdbcTemplate.queryForList(any(String.class), eq(102L)))
                .thenReturn(List.of(Map.of("id", 102L, "sku_code", "SKU-CHAO", "sku_name", "Chảo chống dính", "status", "ACTIVE", "is_deleted", false)));

        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(99L);
            return po;
        });

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                "2026-10-25",
                "Đơn nhập hàng gia dụng đợt 1",
                false,
                List.of(
                        new PurchaseOrderItemRequest(101L, 10, new BigDecimal("500000")),
                        new PurchaseOrderItemRequest(102L, 5, new BigDecimal("200000"))
                )
        );

        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.supplierId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.ORDERED);
        // Total cost: 10 * 500,000 + 5 * 200,000 = 6,000,000 VND
        assertThat(response.totalCost()).isEqualByComparingTo(new BigDecimal("6000000"));
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).skuCode()).isEqualTo("SKU-NOI-COM");
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(response.items().get(1).skuCode()).isEqualTo("SKU-CHAO");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void createPurchaseOrderAsDraftSetsDraftStatus() {
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));
        when(jdbcTemplate.queryForList(any(String.class), eq(101L)))
                .thenReturn(List.of(Map.of("id", 101L, "sku_code", "SKU-NOI-COM", "sku_name", "Nồi cơm", "status", "ACTIVE", "is_deleted", false)));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            po.setId(50L);
            return po;
        });

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                null,
                "Lưu tạm",
                true, // isDraft = true
                List.of(new PurchaseOrderItemRequest(101L, 2, new BigDecimal("300000")))
        );

        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn");

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    }

    @Test
    void createPurchaseOrderThrowsWhenSupplierInactive() {
        activeSupplier.setStatus(SupplierStatus.INACTIVE);
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                null,
                null,
                false,
                List.of(new PurchaseOrderItemRequest(101L, 1, new BigDecimal("100000")))
        );

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn"))
                .isInstanceOf(PurchaseOrderValidationException.class)
                .hasMessageContaining("tạm ngừng hoạt động");
    }

    @Test
    void createPurchaseOrderThrowsWhenEmptyItems() {
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                null,
                null,
                false,
                List.of()
        );

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn"))
                .isInstanceOf(PurchaseOrderValidationException.class)
                .hasMessageContaining("phải có ít nhất một mặt hàng");
    }

    @Test
    void createPurchaseOrderThrowsWhenDuplicateSkuInSamePo() {
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));
        when(jdbcTemplate.queryForList(any(String.class), eq(101L)))
                .thenReturn(List.of(Map.of("id", 101L, "sku_code", "SKU-NOI-COM", "sku_name", "Nồi cơm", "status", "ACTIVE", "is_deleted", false)));

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                null,
                null,
                false,
                List.of(
                        new PurchaseOrderItemRequest(101L, 2, new BigDecimal("100000")),
                        new PurchaseOrderItemRequest(101L, 3, new BigDecimal("100000"))
                )
        );

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn"))
                .isInstanceOf(PurchaseOrderValidationException.class)
                .hasMessageContaining("trùng lặp");
    }

    @Test
    void createPurchaseOrderThrowsWhenSkuDoesNotExist() {
        when(supplierRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(activeSupplier));
        when(jdbcTemplate.queryForList(any(String.class), eq(999L))).thenReturn(List.of());

        CreatePurchaseOrderRequest request = new CreatePurchaseOrderRequest(
                1L,
                null,
                null,
                false,
                List.of(new PurchaseOrderItemRequest(999L, 1, new BigDecimal("100000")))
        );

        assertThatThrownBy(() -> purchaseOrderService.createPurchaseOrder(request, "admin@homemart.vn"))
                .isInstanceOf(PurchaseOrderValidationException.class)
                .hasMessageContaining("không tồn tại");
    }
}
