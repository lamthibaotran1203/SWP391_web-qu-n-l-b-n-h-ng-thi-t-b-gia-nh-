package com.swp391.householdgoods.pos.application;

import com.swp391.householdgoods.orders.api.SalesOrderDetails;
import com.swp391.householdgoods.orders.application.SalesOrderNotFoundException;
import com.swp391.householdgoods.orders.infrastructure.SalesOrderRepository;
import com.swp391.householdgoods.pos.api.PosPaymentMethod;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.ExistingTransaction;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.SaleLine;
import com.swp391.householdgoods.pos.infrastructure.PosTransactionRepository.SaleSku;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosTransactionService {
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("999999999999999999");

    private final PosTransactionRepository repository;
    private final SalesOrderRepository salesOrders;

    public PosTransactionService(
            PosTransactionRepository repository,
            SalesOrderRepository salesOrders) {
        this.repository = repository;
        this.salesOrders = salesOrders;
    }

    @Transactional
    public SalesOrderDetails process(
            String actorEmail,
            UUID submissionKey,
            List<SaleLine> requestedItems,
            PosPaymentMethod paymentMethod) {
        List<SaleLine> items = normalizeItems(requestedItems);
        if (submissionKey == null || paymentMethod == null) {
            throw failure(HttpStatus.BAD_REQUEST, "POS_REQUEST_INVALID",
                    "Mã giao dịch và phương thức thanh toán là bắt buộc.");
        }
        ExistingTransaction previous = repository.findBySubmissionKey(submissionKey).orElse(null);
        if (previous != null) {
            return matchingReplay(previous, items, paymentMethod);
        }

        long actorId = repository.findActiveStaffId(actorEmail)
                .orElseThrow(() -> failure(HttpStatus.UNAUTHORIZED, "POS_STAFF_REQUIRED",
                        "Phiên nhân viên không còn hợp lệ. Vui lòng đăng nhập lại."));
        List<Long> skuIds = items.stream().map(SaleLine::skuId).toList();
        List<SaleSku> skus = repository.lockSellableSkus(skuIds);
        if (skus.size() != skuIds.size()) {
            throw failure(HttpStatus.CONFLICT, "POS_SKU_UNAVAILABLE",
                    "Một hoặc nhiều sản phẩm không còn được phép bán.");
        }

        previous = repository.findBySubmissionKey(submissionKey).orElse(null);
        if (previous != null) {
            return matchingReplay(previous, items, paymentMethod);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < skus.size(); index++) {
            SaleSku sku = skus.get(index);
            SaleLine item = items.get(index);
            if (sku.skuId() != item.skuId()) {
                throw failure(HttpStatus.CONFLICT, "POS_SKU_UNAVAILABLE",
                        "Một hoặc nhiều sản phẩm không còn được phép bán.");
            }
            if (item.quantity() > sku.availableQuantity()) {
                throw failure(HttpStatus.CONFLICT, "POS_STOCK_INSUFFICIENT",
                        "Sản phẩm " + sku.productName() + " chỉ còn "
                                + sku.availableQuantity() + " sản phẩm trong kho.");
            }
            BigDecimal lineTotal = sku.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            total = total.add(lineTotal);
            if (lineTotal.compareTo(MAX_ORDER_AMOUNT) > 0
                    || total.compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw failure(HttpStatus.BAD_REQUEST, "POS_AMOUNT_LIMIT",
                        "Tổng tiền giao dịch vượt quá giới hạn cho phép.");
            }
        }

        String orderCode = "POS-" + UUID.randomUUID().toString().replace("-", "");
        var createdOrder = repository.createOrder(submissionKey, orderCode, total, actorEmail);
        if (createdOrder.isEmpty()) {
            ExistingTransaction replay = repository.findBySubmissionKey(submissionKey)
                    .orElseThrow(() -> failure(HttpStatus.CONFLICT, "POS_REQUEST_CONFLICT",
                            "Không thể ghi nhận giao dịch này. Vui lòng tải lại và thử lại."));
            return matchingReplay(replay, items, paymentMethod);
        }
        long orderId = createdOrder.get();

        for (int index = 0; index < skus.size(); index++) {
            SaleSku sku = skus.get(index);
            SaleLine item = items.get(index);
            if (!repository.deductStock(
                    orderId, submissionKey, sku.skuId(), item.quantity(), actorEmail)) {
                throw failure(HttpStatus.CONFLICT, "POS_STOCK_INSUFFICIENT",
                        "Tồn kho vừa thay đổi. Vui lòng kiểm tra lại giỏ hàng.");
            }
            repository.addOrderItem(orderId, sku, item.quantity(), actorEmail);
        }
        repository.addPayment(orderId, submissionKey, total, paymentMethod, actorEmail);
        repository.addCompletedEvent(orderId, actorId, submissionKey);

        return salesOrders.findDetails(orderId)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderId));
    }

    private List<SaleLine> normalizeItems(List<SaleLine> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw failure(HttpStatus.BAD_REQUEST, "POS_ITEMS_REQUIRED",
                    "Giao dịch phải có ít nhất một sản phẩm.");
        }
        Set<Long> uniqueSkuIds = new HashSet<>();
        List<SaleLine> sorted = new ArrayList<>(requestedItems);
        for (SaleLine item : sorted) {
            if (item == null || item.skuId() < 1 || item.quantity() < 1) {
                throw failure(HttpStatus.BAD_REQUEST, "POS_ITEM_INVALID",
                        "Mã sản phẩm và số lượng phải lớn hơn 0.");
            }
            if (!uniqueSkuIds.add(item.skuId())) {
                throw failure(HttpStatus.BAD_REQUEST, "POS_DUPLICATE_SKU",
                        "Mỗi mã SKU chỉ được xuất hiện một lần trong giao dịch.");
            }
        }
        sorted.sort(Comparator.comparingLong(SaleLine::skuId));
        return List.copyOf(sorted);
    }

    private SalesOrderDetails matchingReplay(
            ExistingTransaction previous,
            List<SaleLine> items,
            PosPaymentMethod paymentMethod) {
        if (!previous.paymentMethod().equals(paymentMethod.name())
                || !previous.items().equals(items)) {
            throw failure(HttpStatus.CONFLICT, "POS_REQUEST_REUSED",
                    "Mã giao dịch đã được dùng cho nội dung bán hàng khác.");
        }
        return salesOrders.findDetails(previous.orderId())
                .orElseThrow(() -> new SalesOrderNotFoundException(previous.orderId()));
    }

    private PosTransactionException failure(HttpStatus status, String code, String message) {
        return new PosTransactionException(status, code, message);
    }
}
