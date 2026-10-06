package com.swp391.householdgoods.catalog.application;

import com.swp391.householdgoods.catalog.api.CatalogPage;
import com.swp391.householdgoods.catalog.api.CatalogReference;
import com.swp391.householdgoods.catalog.api.CatalogSku;
import com.swp391.householdgoods.catalog.infrastructure.CatalogRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogQueryService {
    private static final Set<String> SORTS = Set.of(
            "newest", "oldest", "name_asc", "name_desc", "price_asc", "price_desc");
    private final CatalogRepository repository;

    public CatalogQueryService(CatalogRepository repository) {
        this.repository = repository;
    }

    public CatalogPage page(String search, String brand, String category,
            BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_PAGE_INVALID",
                    "Trang phải từ 0; số mục mỗi trang từ 1 đến 100.");
        }
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_PRICE_INVALID",
                    "Khoảng giá không hợp lệ.");
        }
        String normalizedSearch = normalize(search, 100);
        String normalizedBrand = normalize(brand, 100);
        String normalizedCategory = normalize(category, 100);
        String normalizedSort = sort == null ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        if (!SORTS.contains(normalizedSort)) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_SORT_INVALID",
                    "Kiểu sắp xếp không được hỗ trợ.");
        }
        long total = repository.count(normalizedSearch, normalizedBrand, normalizedCategory,
                minPrice, maxPrice);
        int safePage = total == 0 ? 0 : Math.min(page, (int) ((total - 1) / size));
        return new CatalogPage(repository.page(normalizedSearch, normalizedBrand, normalizedCategory,
                minPrice, maxPrice, normalizedSort, safePage, size), safePage, size, total);
    }

    public CatalogSku sku(long id) {
        return require(repository.bySku(id)).getFirst();
    }

    public List<CatalogSku> product(long id) {
        return require(repository.byProduct(id));
    }

    public List<CatalogReference> categories() {
        return repository.categories();
    }

    public List<CatalogReference> brands() {
        return repository.brands();
    }

    private List<CatalogSku> require(List<CatalogSku> items) {
        if (items.isEmpty()) {
            throw new CatalogException(HttpStatus.NOT_FOUND, "CATALOG_NOT_FOUND",
                    "Không tìm thấy sản phẩm đang được công bố.");
        }
        return items;
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_FILTER_INVALID",
                    "Bộ lọc quá dài.");
        }
        return normalized;
    }
}
