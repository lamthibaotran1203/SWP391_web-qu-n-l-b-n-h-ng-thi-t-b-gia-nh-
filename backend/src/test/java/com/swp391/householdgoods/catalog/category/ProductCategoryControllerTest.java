package com.swp391.householdgoods.catalog.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.catalog.category.api.ProductCategoryController;
import com.swp391.householdgoods.catalog.category.application.CategoryManagementException;
import com.swp391.householdgoods.catalog.category.application.ProductCategoryService;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryOption;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryRequest;
import com.swp391.householdgoods.catalog.category.application.dto.CategoryResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductCategoryController.class)
@Import({SecurityConfig.class, SecurityResponseHandlers.class, ApiProblemWriter.class, ApiExceptionHandler.class})
class ProductCategoryControllerTest {

    private static final String API = "/api/v1/admin/categories";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean ProductCategoryService service;
    @MockitoBean AccountQueryService accountQueryService;
    @MockitoBean CatalogQueryService catalogQueryService;

    @Test
    void adminCanSearchAndViewDetails() throws Exception {
        CategoryResponse category = response(1L, "KITCHEN", "Nhà bếp", false);
        when(service.search("bếp", "ACTIVE", 0, 10))
                .thenReturn(new PageImpl<>(List.of(category), PageRequest.of(0, 10), 1));
        when(service.get(1L)).thenReturn(category);

        mvc.perform(get(API).param("keyword", "bếp").param("status", "ACTIVE")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("KITCHEN"));
        mvc.perform(get(API + "/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nhà bếp"));
    }

    @Test
    void adminCanLoadValidParentOptions() throws Exception {
        when(service.parentOptions(1L)).thenReturn(List.of(
                new CategoryOption(2L, null, "HOME", "Gia dụng")));

        mvc.perform(get(API + "/options").param("excludeId", "1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Gia dụng"));
    }

    @Test
    void adminCanCreateUpdateAndDeactivate() throws Exception {
        CategoryRequest request = new CategoryRequest("KITCHEN", "Nhà bếp", null, null);
        CategoryResponse active = response(1L, "KITCHEN", "Nhà bếp", false);
        CategoryResponse inactive = response(1L, "KITCHEN", "Nhà bếp", true);
        when(service.create(any(), eq("admin"))).thenReturn(active);
        when(service.update(eq(1L), any(), eq("admin"))).thenReturn(active);
        when(service.deactivate(1L, "admin")).thenReturn(inactive);

        mvc.perform(post(API).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isCreated());
        mvc.perform(put(API + "/1").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk());
        mvc.perform(delete(API + "/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void validatesRequestAndMapsBusinessConflicts() throws Exception {
        mvc.perform(post(API).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad code\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        when(service.deactivate(1L, "admin")).thenThrow(new CategoryManagementException(
                HttpStatus.CONFLICT, "CATEGORY_HAS_ACTIVE_PRODUCTS", "Còn sản phẩm hoạt động."));
        mvc.perform(delete(API + "/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_ACTIVE_PRODUCTS"));
    }

    @Test
    void staffCustomerAndAnonymousCannotUseAdminCategoryApi() throws Exception {
        mvc.perform(get(API).with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get(API).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    private CategoryResponse response(Long id, String code, String name, boolean deleted) {
        return new CategoryResponse(id, null, null, code, name, null, deleted,
                deleted ? "INACTIVE" : "ACTIVE", "admin", Instant.now(), "admin", Instant.now());
    }
}
