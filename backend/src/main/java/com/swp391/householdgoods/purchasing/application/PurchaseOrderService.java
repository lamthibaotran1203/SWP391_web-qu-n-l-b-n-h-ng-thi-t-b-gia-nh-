package com.swp391.householdgoods.purchasing.application;

import com.swp391.householdgoods.purchasing.application.dto.CreatePurchaseOrderRequest;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderItemRequest;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderItemResponse;
import com.swp391.householdgoods.purchasing.application.dto.PurchaseOrderResponse;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrder;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderItem;
import com.swp391.householdgoods.purchasing.domain.PurchaseOrderStatus;
import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import com.swp391.householdgoods.purchasing.infrastructure.PurchaseOrderItemRepository;
import com.swp391.householdgoods.purchasing.infrastructure.PurchaseOrderRepository;
import com.swp391.householdgoods.purchasing.infrastructure.SupplierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final JdbcTemplate jdbcTemplate;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            SupplierRepository supplierRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.supplierRepository = supplierRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, String actor) {
        // PRE-1 & PRE-4 & 28.0.E1: Validate Supplier
        Supplier supplier = supplierRepository.findByIdAndIsDeletedFalse(request.supplierId())
                .orElseThrow(() -> new PurchaseOrderValidationException("Nhà cung cấp không tồn tại trên hệ thống."));

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new PurchaseOrderValidationException("Nhà cung cấp đang ở trạng thái tạm ngừng hoạt động (INACTIVE).");
        }

        // POST-2 & 28.0.E3: Validate Empty Purchase Order
        if (request.items() == null || request.items().isEmpty()) {
            throw new PurchaseOrderValidationException("Đơn đặt hàng phải có ít nhất một mặt hàng (PO Item).");
        }

        // Validate items and ensure no duplicate SKUs in the same PO
        Set<Long> seenSkuIds = new HashSet<>();
        List<PurchaseOrderItem> poItems = new ArrayList<>();
        List<PurchaseOrderItemResponse> itemResponses = new ArrayList<>();

        PurchaseOrderStatus initialStatus = request.isDraft()
                ? PurchaseOrderStatus.DRAFT
                : PurchaseOrderStatus.ORDERED;

        Instant expectedDate = null;
        if (request.expectedDate() != null && !request.expectedDate().trim().isEmpty()) {
            try {
                expectedDate = Instant.parse(request.expectedDate().trim());
            } catch (Exception e) {
                // If standard ISO date string without time (e.g., 2026-10-20), parse via LocalDate
                try {
                    expectedDate = LocalDate.parse(request.expectedDate().trim()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
                } catch (Exception ignored) {
                    expectedDate = null;
                }
            }
        }

        String poCode = generateUniquePoCode();

        PurchaseOrder purchaseOrder = new PurchaseOrder(
                supplier,
                poCode,
                Instant.now(),
                expectedDate,
                initialStatus,
                request.note() != null ? request.note().trim() : null,
                actor
        );

        for (PurchaseOrderItemRequest itemReq : request.items()) {
            Long skuId = itemReq.skuId();

            if (!seenSkuIds.add(skuId)) {
                throw new PurchaseOrderValidationException("Đơn hàng không được chứa các dòng SKU trùng lặp (SKU ID: " + skuId + ").");
            }

            if (itemReq.orderedQuantity() <= 0) {
                throw new PurchaseOrderValidationException("Số lượng đặt hàng cho SKU ID " + skuId + " phải lớn hơn 0.");
            }

            if (itemReq.unitCost() == null || itemReq.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new PurchaseOrderValidationException("Đơn giá nhập cho SKU ID " + skuId + " không được âm.");
            }

            // BR-37: VND Monetary Precision (Whole integer VND)
            BigDecimal roundedCost = itemReq.unitCost().setScale(0, RoundingMode.HALF_UP);

            // 28.0.E2: Validate SKU existence from app.hm_sku
            SkuRecord skuRecord = fetchSkuRecord(skuId);
            if (skuRecord == null || skuRecord.isDeleted()) {
                throw new PurchaseOrderValidationException("Mặt hàng SKU ID " + skuId + " không tồn tại hoặc đã bị xóa khỏi danh mục.");
            }

            PurchaseOrderItem item = new PurchaseOrderItem(
                    purchaseOrder,
                    skuId,
                    itemReq.orderedQuantity(),
                    roundedCost,
                    actor
            );

            purchaseOrder.addItem(item);

            itemResponses.add(new PurchaseOrderItemResponse(
                    null,
                    skuId,
                    skuRecord.skuCode(),
                    skuRecord.skuName(),
                    itemReq.orderedQuantity(),
                    0,
                    roundedCost,
                    roundedCost.multiply(BigDecimal.valueOf(itemReq.orderedQuantity()))
            ));
        }

        // Invariant POST-5: Physical inventory is NOT increased at the time of PO creation.
        PurchaseOrder savedPo = purchaseOrderRepository.save(purchaseOrder);

        return PurchaseOrderResponse.fromDomain(savedPo, itemResponses);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> searchPurchaseOrders(
            String keyword,
            Long supplierId,
            PurchaseOrderStatus status,
            Pageable pageable
    ) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return purchaseOrderRepository.searchOrders(cleanKeyword, supplierId, status, pageable)
                .map(po -> {
                    List<PurchaseOrderItemResponse> items = loadItemResponses(po.getId());
                    return PurchaseOrderResponse.fromDomain(po, items);
                });
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new PurchaseOrderValidationException("Không tìm thấy đơn đặt hàng với ID: " + id));

        List<PurchaseOrderItemResponse> items = loadItemResponses(po.getId());
        return PurchaseOrderResponse.fromDomain(po, items);
    }

    private List<PurchaseOrderItemResponse> loadItemResponses(Long poId) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(poId);
        List<PurchaseOrderItemResponse> responses = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            SkuRecord sku = fetchSkuRecord(item.getSkuId());
            String code = sku != null ? sku.skuCode() : "SKU-" + item.getSkuId();
            String name = sku != null ? sku.skuName() : "Sản phẩm";
            responses.add(PurchaseOrderItemResponse.fromDomain(item, code, name));
        }
        return responses;
    }

    private SkuRecord fetchSkuRecord(Long skuId) {
        String sql = "SELECT id, sku_code, sku_name, status, is_deleted FROM app.hm_sku WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, skuId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.getFirst();
        return new SkuRecord(
                ((Number) r.get("id")).longValue(),
                (String) r.get("sku_code"),
                (String) r.get("sku_name"),
                (String) r.get("status"),
                Boolean.TRUE.equals(r.get("is_deleted"))
        );
    }

    private String generateUniquePoCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
            String candidate = "PO-" + dateStr + "-" + randomNum;
            if (!purchaseOrderRepository.existsByPoCode(candidate)) {
                return candidate;
            }
        }
        return "PO-" + dateStr + "-" + System.currentTimeMillis() % 10000;
    }

    private record SkuRecord(Long id, String skuCode, String skuName, String status, boolean isDeleted) {}
}
