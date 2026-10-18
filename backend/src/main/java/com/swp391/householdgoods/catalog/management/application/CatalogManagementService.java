package com.swp391.householdgoods.catalog.management.application;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.domain.ProductStatus;
import com.swp391.householdgoods.catalog.management.api.*;
import com.swp391.householdgoods.catalog.management.api.CatalogWriteRequest.*;
import com.swp391.householdgoods.catalog.management.infrastructure.ManagedProductRepository;
import com.swp391.householdgoods.catalog.management.infrastructure.ManagedSkuRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogManagementService {
    private final ManagedProductRepository products;
    private final ManagedSkuRepository skus;
    private final CatalogWriteValidator validation;
    private final Clock clock;

    public CatalogManagementService(ManagedProductRepository products, ManagedSkuRepository skus,
            CatalogWriteValidator validation, Clock clock) {
        this.products = products;
        this.skus = skus;
        this.validation = validation;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ManagementPage page(String search, ProductStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (search != null && search.length() > 100)) {
            throw error(HttpStatus.BAD_REQUEST, "CATALOG_PAGE_INVALID", "Trang từ 0; số mục 1–100; từ khóa tối đa 100 ký tự.");
        }
        String keyword = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String state = status == null ? "" : status.name();
        return new ManagementPage(products.page(keyword, state, page, size), page, size, products.count(keyword, state));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ManagedProduct detail(long id) {
        return details(requireProduct(id, false));
    }

    @Transactional
    public ManagedProduct create(CreateProduct request, String actor) {
        validation.request(request);
        var fields = validation.product(request.product());
        validateReferences(fields);
        List<SkuFields> initialSkus = request.skus().stream().map(validation::sku).toList();
        initialSkus.forEach(sku -> validateSkuPublication(fields, sku));
        long id = products.insert(fields, clock.instant());
        // One transaction: even a conflict in the final SKU rolls back the master and every earlier SKU.
        for (var sku : initialSkus) insertSku(id, sku, actor);
        return details(requireProduct(id, false));
    }

    @Transactional
    public ManagedProduct editProduct(long id, EditProduct request) {
        validation.request(request);
        var current = requireProduct(id, true);
        requireVersion(current.productVersion(), request.productVersion());
        var fields = validation.product(request.product());
        validateReferences(fields, current);
        if (products.update(id, request.productVersion(), fields, clock.instant()) != 1) throw stale();
        return details(requireProduct(id, false));
    }

    @Transactional
    public ManagedProduct addSku(long id, AddSku request, String actor) {
        validation.request(request);
        var current = requireProduct(id, true);
        requireVersion(current.productVersion(), request.productVersion());
        var fields = validation.sku(request.sku());
        validateSkuPublication(productFields(current), fields);
        insertSku(id, fields, actor);
        products.touch(id, clock.instant());
        return details(requireProduct(id, false));
    }

    @Transactional
    public ManagedProduct editSku(long id, long skuId, EditSku request, String actor) {
        validation.request(request);
        // Lock order: parent product first, then the owned SKU. References are checked under shared locks.
        var current = requireProduct(id, true);
        requireVersion(current.productVersion(), request.productVersion());
        long version = skus.lockVersion(id, skuId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "SKU_NOT_FOUND", "Không tìm thấy phân loại của sản phẩm này."));
        requireVersion(version, request.skuVersion());
        var fields = validation.sku(request.sku());
        validateSkuPublication(productFields(current), fields);
        validateIdentifiers(fields, skuId);
        try {
            if (skus.update(id, skuId, version, fields, actor, clock.instant()) != 1) throw stale();
        } catch (DuplicateKeyException conflict) {
            throw duplicate(conflict);
        }
        products.touch(id, clock.instant());
        return details(requireProduct(id, false));
    }

    private void insertSku(long id, SkuFields fields, String actor) {
        validateIdentifiers(fields, 0);
        try {
            skus.insert(id, fields, actor, clock.instant());
        } catch (DuplicateKeyException conflict) {
            // The database unique constraint remains authoritative when two admins race to use an identifier.
            throw duplicate(conflict);
        }
    }

    private void validateIdentifiers(SkuFields fields, long exceptId) {
        if (skus.codeExists(fields.skuCode(), exceptId)) {
            throw error(HttpStatus.CONFLICT, "SKU_CODE_DUPLICATE", "Mã hàng này đã được sử dụng. Hãy nhập mã khác.");
        }
        if (skus.barcodeExists(fields.barcode(), exceptId)) {
            throw error(HttpStatus.CONFLICT, "SKU_BARCODE_DUPLICATE", "Mã vạch này đã được sử dụng. Hãy nhập mã khác.");
        }
    }

    private void validateReferences(ProductFields fields) {
        validateReferences(fields, null);
    }

    private void validateReferences(ProductFields fields, ManagedProduct prior) {
        // Inactive records may retain an old reference deleted by another UC; new/deleted references cannot be assigned.
        boolean inactiveEdit = prior != null && fields.status() == ProductStatus.INACTIVE;
        if (fields.categoryId() != null && !products.categoryExists(fields.categoryId(), true)
                && !(inactiveEdit && fields.categoryId().equals(prior.categoryId()))) {
            throw error(HttpStatus.BAD_REQUEST, "CATEGORY_INVALID", "Danh mục không tồn tại hoặc đã bị xóa.");
        }
        if (fields.brandId() != null && !products.brandExists(fields.brandId(), true)
                && !(inactiveEdit && fields.brandId().equals(prior.brandId()))) {
            throw error(HttpStatus.BAD_REQUEST, "BRAND_INVALID", "Thương hiệu không tồn tại hoặc đã bị xóa.");
        }
        if (fields.status() == ProductStatus.ACTIVE) requirePublicationData(fields);
    }

    private void validateSkuPublication(ProductFields product, SkuFields sku) {
        if (sku.status() == ProductStatus.ACTIVE) {
            requirePublicationData(product);
            validateReferences(product);
        }
    }

    private void requirePublicationData(ProductFields fields) {
        if (fields.categoryId() == null || fields.brandId() == null || fields.imageUrl() == null || fields.imageUrl().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "PUBLICATION_DATA_MISSING", "Chọn danh mục, thương hiệu và thêm ảnh trước khi chuyển sản phẩm sang Đang bán.");
        }
    }

    private ManagedProduct requireProduct(long id, boolean lock) {
        return products.find(id, lock).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm."));
    }

    private ManagedProduct details(ManagedProduct product) {
        return new ManagedProduct(product.productId(), product.name(), product.description(), product.categoryId(),
                product.brandId(), product.imageUrl(), product.status(), product.productVersion(), skus.byProduct(product.productId()));
    }

    private ProductFields productFields(ManagedProduct product) {
        return new ProductFields(product.name(), product.description(), product.categoryId(), product.brandId(), product.imageUrl(), product.status());
    }

    private void requireVersion(long current, long expected) {
        if (current != expected) throw stale();
    }

    private CatalogException stale() {
        return error(HttpStatus.CONFLICT, "CATALOG_STALE", "Sản phẩm đã được cập nhật ở nơi khác. Nhấn Làm mới rồi kiểm tra lại trước khi lưu.");
    }

    private CatalogException duplicate(DuplicateKeyException conflict) {
        boolean barcode = conflict.getMostSpecificCause().getMessage().contains("hm_sku_barcode_key");
        return error(HttpStatus.CONFLICT, barcode ? "SKU_BARCODE_DUPLICATE" : "SKU_CODE_DUPLICATE",
                barcode ? "Mã vạch này đã được sử dụng. Hãy nhập mã khác." : "Mã hàng này đã được sử dụng. Hãy nhập mã khác.");
    }

    private CatalogException error(HttpStatus status, String code, String message) {
        return new CatalogException(status, code, message);
    }
}
