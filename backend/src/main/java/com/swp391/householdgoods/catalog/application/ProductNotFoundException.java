package com.swp391.householdgoods.catalog.application;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Không tìm thấy sản phẩm với mã " + id + ".");
    }
}
