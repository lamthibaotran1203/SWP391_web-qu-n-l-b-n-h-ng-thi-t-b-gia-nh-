package com.swp391.householdgoods.catalog.category.application;

import com.swp391.householdgoods.catalog.category.application.dto.CategoryOption;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryRequest;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryResponse;
import com.swp391.householdgoods.catalog.category.domain.ProductCategory;
import com.swp391.householdgoods.catalog.category.infrastructure.ProductCategoryRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductCategoryService {

    private final ProductCategoryRepository categories;

    public ProductCategoryService(ProductCategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String keyword, String status, int page, int size) {
        Boolean deleted = parseStatus(status);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
        Page<ProductCategory> result = categories.search(cleanOptional(keyword), deleted, pageable);
        Map<Long, String> parentNames = parentNames(result.getContent());
        List<CategoryResponse> content = result.getContent().stream()
                .map(category -> CategoryResponse.from(category, parentNames.get(category.getParentId())))
                .toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        ProductCategory category = requireCategory(id);
        String parentName = category.getParentId() == null ? null
                : categories.findById(category.getParentId()).map(ProductCategory::getName).orElse(null);
        return CategoryResponse.from(category, parentName);
    }

    @Transactional(readOnly = true)
    public List<CategoryOption> parentOptions(Long excludeId) {
        List<ProductCategory> active = categories.findAllByDeletedFalseOrderByNameAsc();
        Map<Long, Long> parents = new HashMap<>();
        active.forEach(category -> parents.put(category.getId(), category.getParentId()));
        return active.stream()
                .filter(category -> excludeId == null || !isSelfOrDescendant(category.getId(), excludeId, parents))
                .map(category -> new CategoryOption(
                        category.getId(), category.getParentId(), category.getCode(), category.getName()))
                .toList();
    }

    public CategoryResponse create(CategoryRequest request, String actor) {
        String code = cleanCode(request.code());
        String name = cleanRequired(request.name(), "Tên danh mục không được để trống.");
        validateDuplicates(code, name, null);
        validateParent(request.parentId(), null);
        ProductCategory category = new ProductCategory(
                request.parentId(), code, name, cleanOptionalOrNull(request.description()), actor);
        return save(category, request.parentId());
    }

    public CategoryResponse update(Long id, CategoryRequest request, String actor) {
        ProductCategory category = requireCategory(id);
        if (category.isDeleted()) {
            throw error(HttpStatus.BAD_REQUEST, "CATEGORY_DEACTIVATED",
                    "Không thể cập nhật danh mục đã ngừng hoạt động.");
        }
        String code = cleanCode(request.code());
        String name = cleanRequired(request.name(), "Tên danh mục không được để trống.");
        validateDuplicates(code, name, id);
        validateParent(request.parentId(), id);
        category.setCode(code);
        category.setName(name);
        category.setDescription(cleanOptionalOrNull(request.description()));
        category.setParentId(request.parentId());
        category.setUpdatedBy(actor);
        return save(category, request.parentId());
    }

    public CategoryResponse deactivate(Long id, String actor) {
        ProductCategory category = requireCategory(id);
        if (category.isDeleted()) return get(id);
        long activeProducts = categories.countActiveProducts(id);
        if (activeProducts > 0) {
            throw error(HttpStatus.CONFLICT, "CATEGORY_HAS_ACTIVE_PRODUCTS",
                    "Không thể ngừng danh mục vì còn " + activeProducts + " sản phẩm đang hoạt động.");
        }
        long activeChildren = categories.countByParentIdAndDeletedFalse(id);
        if (activeChildren > 0) {
            throw error(HttpStatus.CONFLICT, "CATEGORY_HAS_ACTIVE_CHILDREN",
                    "Không thể ngừng danh mục vì còn " + activeChildren + " danh mục con đang hoạt động.");
        }
        category.setDeleted(true);
        category.setUpdatedBy(actor);
        return save(category, category.getParentId());
    }

    private ProductCategory requireCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục với ID: " + id));
    }

    private void validateDuplicates(String code, String name, Long id) {
        boolean duplicateCode = id == null
                ? categories.existsByCodeIgnoreCase(code)
                : categories.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (duplicateCode) {
            throw error(HttpStatus.CONFLICT, "CATEGORY_CODE_DUPLICATE", "Mã danh mục đã tồn tại: " + code);
        }
        boolean duplicateName = id == null
                ? categories.existsByNameIgnoreCase(name)
                : categories.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicateName) {
            throw error(HttpStatus.CONFLICT, "CATEGORY_NAME_DUPLICATE", "Tên danh mục đã tồn tại: " + name);
        }
    }

    private void validateParent(Long parentId, Long categoryId) {
        if (parentId == null) return;
        if (parentId.equals(categoryId)) {
            throw error(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_INVALID",
                    "Danh mục không thể là cha của chính nó.");
        }
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null) {
            if (!visited.add(cursor) || cursor.equals(categoryId)) {
                throw error(HttpStatus.BAD_REQUEST, "CATEGORY_CYCLE",
                        "Quan hệ danh mục cha tạo thành vòng lặp.");
            }
            ProductCategory parent = requireCategory(cursor);
            if (parent.isDeleted()) {
                throw error(HttpStatus.BAD_REQUEST, "CATEGORY_PARENT_INACTIVE",
                        "Danh mục cha phải đang hoạt động.");
            }
            cursor = parent.getParentId();
        }
    }

    private CategoryResponse save(ProductCategory category, Long parentId) {
        try {
            ProductCategory saved = categories.saveAndFlush(category);
            String parentName = parentId == null ? null
                    : categories.findById(parentId).map(ProductCategory::getName).orElse(null);
            return CategoryResponse.from(saved, parentName);
        } catch (DataIntegrityViolationException exception) {
            throw error(HttpStatus.CONFLICT, "CATEGORY_DUPLICATE",
                    "Mã hoặc tên danh mục đã tồn tại.");
        }
    }

    private Map<Long, String> parentNames(List<ProductCategory> content) {
        Set<Long> ids = new HashSet<>();
        content.stream().map(ProductCategory::getParentId).filter(id -> id != null).forEach(ids::add);
        Map<Long, String> result = new HashMap<>();
        categories.findAllById(ids).forEach(parent -> result.put(parent.getId(), parent.getName()));
        return result;
    }

    private boolean isSelfOrDescendant(Long candidate, Long excluded, Map<Long, Long> parents) {
        Set<Long> visited = new HashSet<>();
        Long cursor = candidate;
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(excluded)) return true;
            cursor = parents.get(cursor);
        }
        return false;
    }

    private Boolean parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> false;
            case "INACTIVE", "DEACTIVATED" -> true;
            default -> throw error(HttpStatus.BAD_REQUEST, "CATEGORY_STATUS_INVALID",
                    "Trạng thái danh mục không hợp lệ.");
        };
    }

    private String cleanCode(String value) {
        return cleanRequired(value, "Mã danh mục không được để trống.").toUpperCase(Locale.ROOT);
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.isBlank()) throw error(HttpStatus.BAD_REQUEST, "CATEGORY_INVALID", message);
        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOptionalOrNull(String value) {
        String cleaned = cleanOptional(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private CategoryManagementException error(HttpStatus status, String code, String message) {
        return new CategoryManagementException(status, code, message);
    }
}
