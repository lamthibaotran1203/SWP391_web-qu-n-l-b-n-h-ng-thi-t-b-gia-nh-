package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.catalog.application.CatalogException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository repository;
    private final CartRemovalService removalService;

    public CartService(CartRepository repository, CartRemovalService removalService) {
        this.repository = repository;
        this.removalService = removalService;
    }

    public record Item(long skuId, long productId, String name, String skuName, String skuCode,
            String imageUrl, int quantity, String unitPrice, String lineTotal, int availableQuantity,
            boolean eligible, boolean available, UUID version) {}
    public record Cart(List<Item> items, String subtotal, boolean checkoutEligible) {}
    public record RemovalResult(boolean removed, Cart cart, boolean refreshRequired, String message) {}

    @Transactional(readOnly = true)
    public Cart view(String email) {
        long customerId = requireCustomer(email, false);
        return readCart(customerId);
    }

    @Transactional
    public Cart addItem(String email, long skuId, int quantity, UUID requestId) {
        // Khóa theo Customer: mọi thay đổi giỏ của cùng người được xử lý lần lượt.
        long customerId = requireCustomer(email, true);
        validateQuantity(quantity);
        if (requestId == null) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CART_REQUEST_ID_REQUIRED",
                    "Yêu cầu thêm giỏ hàng phải có mã chống gửi lặp.");
        }

        List<CartRepository.AddRequest> accepted = repository.findAddRequest(customerId, requestId);
        if (!accepted.isEmpty()) {
            CartRepository.AddRequest previous = accepted.getFirst();
            if (previous.skuId() != skuId || previous.quantity() != quantity) {
                throw new CatalogException(HttpStatus.CONFLICT, "CART_REQUEST_REUSED",
                        "Mã yêu cầu đã được dùng cho nội dung khác.");
            }
            // Request đã được lưu thành công: chỉ đọc lại, không cộng số lượng lần nữa.
            return readCart(customerId);
        }

        int stock = requireEligibleStock(skuId);
        long desiredQuantity = repository.findQuantity(customerId, skuId) + quantity;
        validateStock(desiredQuantity, stock);
        repository.saveQuantity(customerId, skuId, (int) desiredQuantity, email);
        repository.saveAddRequest(customerId, requestId, skuId, quantity);
        return readCart(customerId);
    }

    @Transactional
    public Cart updateItem(String email, long skuId, int quantity, UUID expectedVersion) {
        long customerId = requireCustomer(email, true);
        List<CartRepository.Line> lines = repository.findLine(customerId, skuId);
        if (lines.isEmpty()) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                    "Sản phẩm không có trong giỏ hàng.");
        }
        validateQuantity(quantity);
        CartRepository.Line line = lines.getFirst();
        if (quantity == line.quantity()) {
            return readCart(customerId); // Không ghi, kể cả khi SKU vừa ngừng bán.
        }
        if (!line.version().equals(expectedVersion)) {
            throw new CatalogException(HttpStatus.CONFLICT, "CART_CHANGED",
                    "Giỏ hàng đã thay đổi ở nơi khác. Vui lòng tải lại rồi thử lại.");
        }

        int stock = requireEligibleStock(skuId);
        validateStock(quantity, stock);
        repository.saveQuantity(customerId, skuId, quantity, email);
        return readCart(customerId);
    }

    // Không gộp tải lại vào transaction xóa: lỗi tải không được hoàn tác lần xóa đã commit.
    public RemovalResult remove(String email, List<Long> skuIds) {
        long customerId = removalService.removeItems(email, skuIds);
        try {
            return new RemovalResult(true, readCart(customerId), false, "Đã xóa sản phẩm đã chọn.");
        } catch (DataAccessException | TransactionException failure) {
            return new RemovalResult(true, null, true,
                    "Đã xóa sản phẩm. Chưa tải được giỏ hàng mới, vui lòng tải lại.");
        }
    }

    private long requireCustomer(String email, boolean lockForUpdate) {
        List<Long> customerIds = repository.findActiveCustomerIds(email, lockForUpdate);
        if (customerIds.isEmpty()) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "CART_SESSION_EXPIRED",
                    "Vui lòng đăng nhập lại để sử dụng giỏ hàng.");
        }
        return customerIds.getFirst();
    }

    private int requireEligibleStock(long skuId) {
        List<Integer> stock = repository.findEligibleStock(skuId);
        if (stock.isEmpty()) {
            throw new CatalogException(HttpStatus.CONFLICT, "CART_SKU_INELIGIBLE",
                    "Phiên bản sản phẩm không còn được bán.");
        }
        return stock.getFirst();
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw invalidQuantity("Số lượng phải là số nguyên dương.");
        }
    }

    private void validateStock(long quantity, int availableStock) {
        if (quantity > availableStock) {
            throw invalidQuantity("Số lượng vượt quá tồn kho hiện tại (" + availableStock + ").");
        }
    }

    private Cart readCart(long customerId) {
        List<Item> items = repository.findItems(customerId);
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean checkoutEligible = !items.isEmpty();
        for (Item item : items) {
            subtotal = subtotal.add(new BigDecimal(item.lineTotal()));
            if (!item.available()) {
                checkoutEligible = false;
            }
        }
        return new Cart(items, subtotal.toPlainString(), checkoutEligible);
    }

    private CatalogException invalidQuantity(String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, "CART_QUANTITY_INVALID", message);
    }
}
