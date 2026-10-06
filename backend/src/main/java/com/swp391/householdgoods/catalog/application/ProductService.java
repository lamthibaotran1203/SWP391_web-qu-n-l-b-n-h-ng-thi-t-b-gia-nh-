package com.swp391.householdgoods.catalog.application;

import com.swp391.householdgoods.catalog.application.dto.CreateProductRequest;
import com.swp391.householdgoods.catalog.application.dto.ProductResponse;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.infrastructure.ProductWriteRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('MANAGER')")
public class ProductService {
    private final ProductWriteRepository repository;

    public ProductService(ProductWriteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.sku().trim();
        ProductStatus status = requestedStatus(request);
        validatePublication(request, status);
        if (request.stockQuantity() != 0) throw new IllegalArgumentException("Tạo SKU với tồn kho 0; nhập kho qua nghiệp vụ kho.");
        Long category = repository.referenceId("hm_category", normalize(request.category()));
        Long brand = repository.referenceId("hm_brand", normalize(request.brand()));
        try {
            repository.guardSku(sku, null);
            return repository.insert(request, category, brand, status);
        } catch (DuplicateKeyException exception) { throw duplicate(); }
    }

    @Transactional
    public ProductResponse update(Long id, CreateProductRequest request) {
        String sku = request.sku().trim();
        ProductStatus status = requestedStatus(request);
        validatePublication(request, status);
        Long category = repository.referenceId("hm_category", normalize(request.category()));
        Long brand = repository.referenceId("hm_brand", normalize(request.brand()));
        try {
            repository.guardSku(sku, id);
            ProductResponse current = repository.lock(id);
            checkVersions(current, request.productVersion(), request.skuVersion());
            if (request.stockQuantity() != current.stockQuantity()) {
                throw new IllegalArgumentException("Không sửa tồn kho qua API catalog; sử dụng nghiệp vụ kho.");
            }
            return repository.update(current, request, category, brand, status);
        } catch (DuplicateKeyException exception) { throw duplicate(); }
    }

    @Transactional
    public void delete(Long id, Long productVersion, Long skuVersion) {
        ProductResponse current = repository.lock(id);
        checkVersions(current, productVersion, skuVersion);
        repository.softDelete(id);
    }

    private void checkVersions(ProductResponse current, Long productVersion, Long skuVersion) {
        if (productVersion == null || skuVersion == null) throw new IllegalArgumentException("Cần productVersion và skuVersion khi sửa/xóa.");
        if (!productVersion.equals(current.productVersion()) || !skuVersion.equals(current.skuVersion())) {
            throw new CatalogException(HttpStatus.CONFLICT, "CATALOG_STALE", "Dữ liệu đã thay đổi. Vui lòng tải lại.");
        }
    }

    private CatalogException duplicate() {
        return new CatalogException(HttpStatus.CONFLICT, "CATALOG_DUPLICATE", "SKU hoặc barcode đã tồn tại.");
    }

    private ProductStatus requestedStatus(CreateProductRequest request) {
        return request.status() == null ? ProductStatus.ACTIVE : request.status();
    }

    private void validatePublication(CreateProductRequest request, ProductStatus status) {
        if (status == ProductStatus.ACTIVE
                && (normalize(request.brand()) == null || normalize(request.category()) == null
                || normalize(request.imageUrl()) == null)) {
            throw new IllegalArgumentException("Sản phẩm ACTIVE phải có thương hiệu, danh mục và hình ảnh.");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
