package com.swp391.householdgoods.catalog.management.api;

import com.swp391.householdgoods.catalog.domain.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Catalogue fields only: clients cannot supply inventory, audit fields or a parent SKU ID. */
public final class CatalogWriteRequest {
    private CatalogWriteRequest() {}

    public record ProductFields(
            @NotBlank(message = "Nhập tên sản phẩm.") @Size(min = 2, max = 200) String name,
            @Size(max = 10000) String description,
            @Positive Long categoryId,
            @Positive Long brandId,
            @Size(max = 1000) String imageUrl,
            @NotNull(message = "Chọn trạng thái sản phẩm.") ProductStatus status) {}

    public record SkuFields(
            @NotBlank(message = "Nhập mã hàng.") @Size(max = 100) String skuCode,
            @NotBlank(message = "Nhập mã vạch nội bộ.") @Size(max = 100) String barcode,
            @NotBlank(message = "Nhập tên phân loại.") @Size(max = 255) String skuName,
            @Size(max = 20) Map<String, String> attributes,
            @Size(max = 512) String imageUrl,
            @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 0) BigDecimal originalPrice,
            @NotNull @DecimalMin("0") @Digits(integer = 18, fraction = 0) BigDecimal sellingPrice,
            @NotNull @Min(0) Integer minStockThreshold,
            @NotNull(message = "Chọn trạng thái phân loại.") ProductStatus status) {}

    public record CreateProduct(
            @NotNull @Valid ProductFields product,
            @NotEmpty @Size(max = 50) List<@NotNull @Valid SkuFields> skus) {}

    public record EditProduct(
            @NotNull @Min(0) Long productVersion,
            @NotNull @Valid ProductFields product) {}

    public record AddSku(
            @NotNull @Min(0) Long productVersion,
            @NotNull @Valid SkuFields sku) {}

    public record EditSku(
            @NotNull @Min(0) Long productVersion,
            @NotNull @Min(0) Long skuVersion,
            @NotNull @Valid SkuFields sku) {}
}
