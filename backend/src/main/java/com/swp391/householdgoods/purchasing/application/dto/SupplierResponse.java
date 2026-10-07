package com.swp391.householdgoods.purchasing.application.dto;

import com.swp391.householdgoods.purchasing.domain.Supplier;
import com.swp391.householdgoods.purchasing.domain.SupplierStatus;
import java.time.Instant;

public record SupplierResponse(
        Long id,
        String code,
        String taxCode,
        String name,
        String contactName,
        String phone,
        String email,
        String address,
        SupplierStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse fromDomain(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getTaxCode(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getStatus(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
