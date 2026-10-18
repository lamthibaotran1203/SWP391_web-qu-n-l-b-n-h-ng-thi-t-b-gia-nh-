package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.catalog.application.CatalogException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartRepository repository;

    public CartService(CartRepository repository) {
        this.repository = repository;
    }

    public record Item(long skuId, long productId, String name, String skuName, String skuCode,
            String imageUrl, int quantity, String unitPrice, String lineTotal, int availableQuantity,
            boolean eligible, boolean available) {}

    public record Cart(List<Item> items, String subtotal, boolean checkoutEligible) {}

    @Transactional(readOnly = true)
    public Cart view(String email) {
        long customerId = requireCustomer(email, false);
        return readCart(customerId);
    }

    @Transactional
    public Cart save(String email, long skuId, int quantity, boolean add) {
        // Khóa khách hàng để hai thao tác cùng lúc không ghi đè số lượng của nhau.
        long customerId = requireCustomer(email, true);
        if (quantity < 1) {
            throw invalidQuantity("Số lượng phải là số nguyên dương.");
        }

        long existingQuantity = repository.findQuantity(customerId, skuId);
        long desiredQuantity = quantity;
        if (add) {
            // Thêm vào giỏ: cộng vào dòng đã có. Cập nhật: thay bằng số lượng mới.
            desiredQuantity = existingQuantity + quantity;
        }

        List<Integer> stockResults = repository.findEligibleStock(skuId);
        if (stockResults.isEmpty()) {
            throw new CatalogException(HttpStatus.CONFLICT, "CART_SKU_INELIGIBLE",
                    "Phiên bản sản phẩm không còn được bán.");
        }

        int availableStock = stockResults.getFirst();
        if (desiredQuantity > availableStock) {
            throw invalidQuantity("Số lượng vượt quá tồn kho hiện tại (" + availableStock + ").");
        }
        if (!add && existingQuantity == 0) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                    "Sản phẩm không có trong giỏ hàng.");
        }

        repository.saveQuantity(customerId, skuId, (int) desiredQuantity, email);
        return readCart(customerId);
    }

    @Transactional
    public Cart remove(String email, List<Long> skuIds) {
        long customerId = requireCustomer(email, true);
        for (Long skuId : skuIds) {
            repository.deleteItem(customerId, skuId);
        }
        return readCart(customerId);
    }

    private long requireCustomer(String email, boolean lockForUpdate) {
        List<Long> customerIds = repository.findActiveCustomerIds(email, lockForUpdate);
        if (customerIds.isEmpty()) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "CART_SESSION_EXPIRED",
                    "Vui lòng đăng nhập lại để sử dụng giỏ hàng.");
        }
        return customerIds.getFirst();
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
