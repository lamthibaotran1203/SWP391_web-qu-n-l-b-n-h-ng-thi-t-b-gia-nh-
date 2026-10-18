package com.swp391.householdgoods.catalog.application;

import com.swp391.householdgoods.catalog.api.CatalogueProduct;
import com.swp391.householdgoods.catalog.api.PublicCategory;
import com.swp391.householdgoods.catalog.infrastructure.CatalogueBrowseRepository;
import com.swp391.householdgoods.catalog.infrastructure.PublicCategoryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CatalogueBrowseService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999999999999");
    private static final Set<String> SUPPORTED_SORTS = Set.of(
            "newest", "oldest", "name_asc", "name_desc", "price_asc", "price_desc");
    private static final Set<String> SUPPORTED_AVAILABILITY = Set.of(
            "any", "in_stock", "out_of_stock");

    private final CatalogueBrowseRepository products;
    private final PublicCategoryRepository categories;

    public CatalogueBrowseService(CatalogueBrowseRepository products, PublicCategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public record Search(String search, Long brandId, Long categoryId, BigDecimal minPrice,
            BigDecimal maxPrice, String availability, String sort, int page, int size) {}

    public record Result(List<CatalogueProduct> items, int page, int size, long totalElements) {}

    public Result browse(Search input) {
        validatePagination(input.page(), input.size());
        String keyword = normalizeKeyword(input.search());
        validateOptions(input.sort(), input.availability());
        validatePriceRange(input.minPrice(), input.maxPrice());
        validateReferences(input.categoryId(), input.brandId());

        Search search = new Search(keyword, input.brandId(), input.categoryId(), input.minPrice(),
                input.maxPrice(), input.availability(), input.sort(), input.page(), input.size());
        long total = products.count(search);
        int page = resolvePage(input.page(), input.size(), total);
        List<CatalogueProduct> items = products.page(search, page);
        return new Result(items, page, input.size(), total);
    }

    public List<PublicCategory> categories(Long parentId, boolean roots) {
        if (parentId != null && !categories.visible(parentId)) {
            throw invalid("parentId", "Danh mục cha không tồn tại hoặc không công khai.");
        }

        List<PublicCategory> result = new ArrayList<>();
        for (PublicCategory category : categories.all()) {
            if (parentId != null) {
                if (parentId.equals(category.parentId())) {
                    result.add(category);
                }
            } else if (!roots || category.parentId() == null) {
                result.add(category);
            }
        }
        return List.copyOf(result);
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw invalid("page", "Trang phải từ 0; số mục mỗi trang từ 1 đến 100.");
        }
    }

    private String normalizeKeyword(String keyword) {
        String normalized = "";
        if (keyword != null) {
            normalized = keyword.trim().replaceAll("\\s+", " ");
        }
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw invalid("search", "Từ khóa tối đa 100 ký tự.");
        }
        return normalized;
    }

    private void validateOptions(String sort, String availability) {
        if (!SUPPORTED_SORTS.contains(sort)) {
            throw invalid("sort", "Kiểu sắp xếp không được hỗ trợ.");
        }
        if (!SUPPORTED_AVAILABILITY.contains(availability)) {
            throw invalid("availability", "Bộ lọc tồn kho không hợp lệ.");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (isInvalidPrice(minPrice) || isInvalidPrice(maxPrice)) {
            throw invalid("price", "Giá phải là số nguyên VND không âm, tối đa 18 chữ số.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw invalid("price", "Giá từ không được lớn hơn giá đến.");
        }
    }

    private boolean isInvalidPrice(BigDecimal price) {
        if (price == null) {
            return false;
        }
        boolean negative = price.signum() < 0;
        boolean fractional = price.stripTrailingZeros().scale() > 0;
        boolean exceedsLimit = price.compareTo(MAX_PRICE) > 0;
        return negative || fractional || exceedsLimit;
    }

    private void validateReferences(Long categoryId, Long brandId) {
        if (categoryId != null && (categoryId < 1 || !categories.visible(categoryId))) {
            throw invalid("categoryId", "Danh mục không tồn tại hoặc hiện không công khai.");
        }
        if (brandId != null && (brandId < 1 || !categories.brandExists(brandId))) {
            throw invalid("brandId", "Thương hiệu không tồn tại hoặc đã ngừng dùng.");
        }
    }

    private int resolvePage(int requestedPage, int size, long total) {
        if (total == 0) {
            return 0;
        }
        long lastPage = (total - 1) / size;
        return (int) Math.min(requestedPage, lastPage);
    }

    private CatalogException invalid(String field, String message) {
        String code = "CATALOG_" + field.toUpperCase(Locale.ROOT) + "_INVALID";
        return new CatalogException(HttpStatus.BAD_REQUEST, code, message);
    }
}
