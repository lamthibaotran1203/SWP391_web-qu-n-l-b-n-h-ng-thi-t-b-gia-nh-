package com.swp391.householdgoods.catalog.api;

import com.swp391.householdgoods.catalog.application.CatalogException;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService.Search;
import com.swp391.householdgoods.common.api.ApiEnvelope;
import com.swp391.householdgoods.common.api.ApiProblem;
import com.swp391.householdgoods.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogueBrowseController {
    private static final Set<String> PRODUCT_PARAMETERS = Set.of(
            "search", "brandId", "categoryId", "minPrice", "maxPrice",
            "availability", "sort", "page", "size");
    private static final Set<String> CATEGORY_PARAMETERS = Set.of("parentId", "roots");
    private final CatalogueBrowseService service;

    public CatalogueBrowseController(CatalogueBrowseService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public ApiEnvelope<CatalogueBrowseService.Result> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "any") String availability,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            HttpServletRequest request) {
        validateParameters(request, PRODUCT_PARAMETERS);
        Search criteria = new Search(search, brandId, categoryId, minPrice, maxPrice,
                availability, sort, page, size);
        return executeQuery(() -> service.browse(criteria));
    }

    @GetMapping("/public-categories")
    public ApiEnvelope<List<PublicCategory>> categories(
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "false") boolean roots,
            HttpServletRequest request) {
        validateParameters(request, CATEGORY_PARAMETERS);
        return executeQuery(() -> service.categories(parentId, roots));
    }

    private void validateParameters(HttpServletRequest request, Set<String> allowed) {
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            String name = parameter.getKey();
            boolean unsupported = !allowed.contains(name);
            boolean repeated = parameter.getValue().length != 1;
            if (unsupported || repeated) {
                throw new CatalogException(HttpStatus.BAD_REQUEST, "CATALOG_FILTER_INVALID",
                        "Tiêu chí không được hỗ trợ hoặc bị lặp: " + name);
            }
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiProblem> invalidType(
            MethodArgumentTypeMismatchException failure, HttpServletRequest request) {
        String field = failure.getName();
        String message = "Tiêu chí " + field + " không đúng định dạng.";
        ApiProblem problem = ApiProblem.of(400, "Bad Request", "CATALOG_FILTER_INVALID",
                message, Map.of(field, message), TraceIdFilter.getTraceId(request));
        return ResponseEntity.badRequest().body(problem);
    }

    // query là lời gọi Service; lỗi truy cập dữ liệu được đổi thành thông báo có thể thử lại.
    private <T> ApiEnvelope<T> executeQuery(Supplier<T> query) {
        try {
            T result = query.get();
            return new ApiEnvelope<>(result);
        } catch (DataAccessException | TransactionException failure) {
            throw new CatalogException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                    "Chưa thể tải danh mục hoặc sản phẩm. Vui lòng thử lại.");
        }
    }
}
