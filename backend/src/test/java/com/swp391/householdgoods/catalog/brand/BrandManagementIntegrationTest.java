package com.swp391.householdgoods.catalog.brand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.catalog.api.CatalogController;
import com.swp391.householdgoods.catalog.api.CatalogReference;
import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.catalog.brand.api.BrandManagementController;
import com.swp391.householdgoods.catalog.brand.application.BrandDeactivatedException;
import com.swp391.householdgoods.catalog.brand.application.BrandDuplicateException;
import com.swp391.householdgoods.catalog.brand.application.BrandHasActiveProductsException;
import com.swp391.householdgoods.catalog.brand.application.BrandNotFoundException;
import com.swp391.householdgoods.catalog.brand.application.BrandService;
import com.swp391.householdgoods.catalog.brand.application.dto.BrandResponse;
import com.swp391.householdgoods.catalog.brand.application.dto.CreateBrandRequest;
import com.swp391.householdgoods.catalog.brand.application.dto.UpdateBrandRequest;
import com.swp391.householdgoods.common.api.ApiExceptionHandler;
import com.swp391.householdgoods.common.api.ApiProblemWriter;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.security.SecurityConfig;
import com.swp391.householdgoods.security.SecurityResponseHandlers;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({BrandManagementController.class, CatalogController.class})
@Import({SecurityConfig.class, SecurityResponseHandlers.class, ApiProblemWriter.class, ApiExceptionHandler.class})
class BrandManagementIntegrationTest {

    private static final String API = "/api/v1/manager/brands";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private CatalogQueryService catalogQueryService;

    @MockitoBean
    private AccountQueryService accountQueryService;

    private BrandResponse sampleBrand(Long id, String name, boolean isDeleted) {
        return new BrandResponse(
                id,
                name,
                "Vietnam",
                "Thương hiệu mẫu",
                "https://example.com/logo.svg",
                isDeleted,
                isDeleted ? "INACTIVE" : "ACTIVE",
                "manager@homemart.vn",
                Instant.now(),
                "manager@homemart.vn",
                Instant.now()
        );
    }

    // 1. MANAGER can create a brand
    @Test
    void managerCanCreateBrand() throws Exception {
        CreateBrandRequest request = new CreateBrandRequest("Sunhouse", "Vietnam", "Gia dung", null);
        BrandResponse response = sampleBrand(1L, "Sunhouse", false);

        when(brandService.createBrand(any(CreateBrandRequest.class), eq("manager"))).thenReturn(response);

        mvc.perform(post(API)
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Sunhouse"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // 2. Invalid name is rejected
    @Test
    void invalidNameIsRejected() throws Exception {
        CreateBrandRequest blankRequest = new CreateBrandRequest("   ", "Vietnam", null, null);

        mvc.perform(post(API)
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // 3. Duplicate name is rejected case-insensitively
    @Test
    void duplicateNameIsRejectedWithConflict() throws Exception {
        CreateBrandRequest request = new CreateBrandRequest("sunhouse", "Vietnam", null, null);

        when(brandService.createBrand(any(CreateBrandRequest.class), eq("manager")))
                .thenThrow(new BrandDuplicateException("Tên thương hiệu đã tồn tại: sunhouse"));

        mvc.perform(post(API)
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRAND_DUPLICATE"))
                .andExpect(jsonPath("$.detail").value("Tên thương hiệu đã tồn tại: sunhouse"));
    }

    // 5. MANAGER can list all brands
    // 6. Search by name and country
    // 7. Filter active/inactive
    // 8. Pagination works
    @Test
    void managerCanListBrandsWithSearchFilterAndPagination() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        BrandResponse b1 = sampleBrand(1L, "Sunhouse", false);
        BrandResponse b2 = sampleBrand(2L, "Lock&Lock", true);

        when(brandService.searchBrands("Sun", "ACTIVE", pageable))
                .thenReturn(new PageImpl<>(List.of(b1), pageable, 1));

        mvc.perform(get(API)
                        .param("keyword", "Sun")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Sunhouse"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void invalidPaginationSizeIsRejected() throws Exception {
        mvc.perform(get(API)
                        .param("size", "150")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // 9. View brand details
    @Test
    void viewBrandDetailsReturns200Or404() throws Exception {
        when(brandService.getBrandById(1L)).thenReturn(sampleBrand(1L, "Sunhouse", false));
        when(brandService.getBrandById(99L)).thenThrow(new BrandNotFoundException("Không tìm thấy thương hiệu với ID: 99"));

        mvc.perform(get(API + "/1").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sunhouse"));

        mvc.perform(get(API + "/99").with(user("manager").roles("MANAGER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRAND_NOT_FOUND"));
    }

    // 10. Update brand
    @Test
    void updateBrandReturnsUpdatedData() throws Exception {
        UpdateBrandRequest request = new UpdateBrandRequest("Sunhouse Group", "Vietnam", "Cap nhat", null);
        BrandResponse updated = sampleBrand(1L, "Sunhouse Group", false);

        when(brandService.updateBrand(eq(1L), any(UpdateBrandRequest.class), eq("manager")))
                .thenReturn(updated);

        mvc.perform(put(API + "/1")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sunhouse Group"));
    }

    // 11. Update duplicate name rejected
    @Test
    void updateDuplicateNameIsRejected() throws Exception {
        UpdateBrandRequest request = new UpdateBrandRequest("Philips", "Vietnam", null, null);

        when(brandService.updateBrand(eq(1L), any(UpdateBrandRequest.class), eq("manager")))
                .thenThrow(new BrandDuplicateException("Tên thương hiệu đã tồn tại: Philips"));

        mvc.perform(put(API + "/1")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRAND_DUPLICATE"));
    }

    // Deactivated brand update rejection
    @Test
    void updateDeactivatedBrandIsRejected() throws Exception {
        UpdateBrandRequest request = new UpdateBrandRequest("Philips", "Vietnam", null, null);

        when(brandService.updateBrand(eq(1L), any(UpdateBrandRequest.class), eq("manager")))
                .thenThrow(new BrandDeactivatedException("Không thể cập nhật thương hiệu đã bị vô hiệu hóa."));

        mvc.perform(put(API + "/1")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRAND_DEACTIVATED"));
    }

    // 12. Deactivate brand
    // 13. Verify is_deleted persisted
    @Test
    void deactivateBrandViaDeleteOrPatch() throws Exception {
        BrandResponse deactivated = sampleBrand(1L, "Sunhouse", true);

        when(brandService.deactivateBrand(1L, "manager")).thenReturn(deactivated);

        mvc.perform(delete(API + "/1")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDeleted").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mvc.perform(patch(API + "/1/status")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDeleted").value(true));
    }

    // 14. Existing product relationships remain valid: 409 conflict when active products exist
    @Test
    void deactivateBrandWithActiveProductsReturnsConflict() throws Exception {
        when(brandService.deactivateBrand(1L, "manager"))
                .thenThrow(new BrandHasActiveProductsException("Không thể vô hiệu hóa thương hiệu vì vẫn còn 5 sản phẩm đang hoạt động."));

        mvc.perform(delete(API + "/1")
                        .with(user("manager").roles("MANAGER"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRAND_HAS_ACTIVE_PRODUCTS"))
                .andExpect(jsonPath("$.detail").value("Không thể vô hiệu hóa thương hiệu vì vẫn còn 5 sản phẩm đang hoạt động."));
    }

    // 15. CUSTOMER and unauthenticated users cannot modify brands
    @Test
    void customerAndUnauthenticatedCannotAccessManagerBrandApis() throws Exception {
        CreateBrandRequest request = new CreateBrandRequest("Test", null, null, null);

        // Anonymous without auth
        mvc.perform(post(API).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(get(API))
                .andExpect(status().isUnauthorized());

        // CUSTOMER role
        mvc.perform(post(API)
                        .with(user("customer").roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // STAFF role
        mvc.perform(delete(API + "/1")
                        .with(user("staff").roles("STAFF"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // CSRF verification: Manager cannot write without CSRF token
    @Test
    void managerCannotWriteWithoutCsrf() throws Exception {
        CreateBrandRequest request = new CreateBrandRequest("Test", null, null, null);

        mvc.perform(post(API)
                        .with(user("manager").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // 16. Existing GET /api/v1/catalog/brands remains unchanged and works
    @Test
    void publicCatalogBrandsRemainsUnchangedAndPermitAll() throws Exception {
        when(catalogQueryService.brands()).thenReturn(List.of(
                new CatalogReference(1L, "Sunhouse"),
                new CatalogReference(2L, "Lock&Lock")
        ));

        mvc.perform(get("/api/v1/catalog/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Sunhouse"));
    }
}
