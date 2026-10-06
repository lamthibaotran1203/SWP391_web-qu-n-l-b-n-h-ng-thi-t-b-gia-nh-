package com.swp391.householdgoods.catalog.application.dto;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(min = 2, max = 200) String name,
        @Size(max = 100) String brand,
        @Size(max = 100) String category,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @Min(0) int stockQuantity,
        String description,
        @Size(max = 1000) String imageUrl,
        ProductStatus status) {
}
