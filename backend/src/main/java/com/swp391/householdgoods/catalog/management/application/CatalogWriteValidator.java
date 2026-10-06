package com.swp391.householdgoods.catalog.management.application;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.ProductFields;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.SkuFields;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Small normalization/validation helper; all reference, state and version rules remain in the service. */
@Component
public class CatalogWriteValidator {
    private final Validator validator;

    public CatalogWriteValidator(Validator validator) {
        this.validator = validator;
    }

    public void request(Object request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw invalid("Thông tin sản phẩm chưa hợp lệ. Kiểm tra tên, mã hàng, giá và mức cảnh báo sắp hết hàng.");
        }
    }

    public ProductFields product(ProductFields fields) {
        var normalized = new ProductFields(trim(fields.name()), trim(fields.description()), fields.categoryId(),
                fields.brandId(), image(fields.imageUrl()), fields.status());
        request(normalized);
        return normalized;
    }

    public SkuFields sku(SkuFields fields) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (fields.attributes() != null) {
            for (var entry : fields.attributes().entrySet()) {
                String key = trim(entry.getKey());
                String value = trim(entry.getValue());
                if (key == null || value == null || key.length() > 50 || value.length() > 255
                        || attributes.putIfAbsent(key, value) != null) {
                    throw invalid("Tên thông số cần 1–50 ký tự, giá trị cần 1–255 ký tự. Không nhập trùng tên thông số.");
                }
            }
        }
        var normalized = new SkuFields(trim(fields.skuCode()), trim(fields.barcode()), trim(fields.skuName()),
                attributes, image(fields.imageUrl()), fields.originalPrice(), fields.sellingPrice(),
                fields.minStockThreshold(), fields.status());
        request(normalized);
        return normalized;
    }

    private String image(String value) {
        String normalized = trim(value);
        // Store an existing image URL/path; do not build an upload service or allow executable URL schemes.
        if (normalized != null && !(normalized.startsWith("/") && !normalized.startsWith("//"))
                && !normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            throw invalid("Liên kết ảnh chưa hợp lệ. Hãy dán liên kết ảnh bắt đầu bằng https:// hoặc chọn đường dẫn ảnh của cửa hàng.");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CatalogException invalid(String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_DATA_INVALID", message);
    }
}
