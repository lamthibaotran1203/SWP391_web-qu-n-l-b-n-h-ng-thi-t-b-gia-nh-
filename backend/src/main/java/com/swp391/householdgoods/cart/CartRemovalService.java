package com.swp391.householdgoods.cart;

import com.swp391.householdgoods.catalog.application.CatalogException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartRemovalService {
    private final CartRepository repository;

    public CartRemovalService(CartRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public long removeItems(String email, List<Long> skuIds) {
        List<Long> customerIds = repository.findActiveCustomerIds(email, true);
        if (customerIds.isEmpty()) {
            throw new CatalogException(HttpStatus.UNAUTHORIZED, "CART_SESSION_EXPIRED",
                    "Vui lòng đăng nhập lại để sử dụng giỏ hàng.");
        }
        long customerId = customerIds.getFirst();
        for (Long skuId : skuIds) {
            repository.deleteItem(customerId, skuId);
        }
        return customerId;
    }
}
