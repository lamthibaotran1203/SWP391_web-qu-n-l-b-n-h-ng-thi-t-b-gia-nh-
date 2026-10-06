package com.swp391.householdgoods.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;

import com.swp391.householdgoods.catalog.application.CatalogQueryService;
import com.swp391.householdgoods.catalog.application.ProductService;
import com.swp391.householdgoods.catalog.web.ProductController;
import com.swp391.householdgoods.common.api.ApiProblemWriter;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.security.SecurityConfig;
import com.swp391.householdgoods.security.SecurityResponseHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ProductController.class, com.swp391.householdgoods.catalog.api.CatalogController.class})
@Import({SecurityConfig.class, SecurityResponseHandlers.class, ApiProblemWriter.class})
class ProductSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;
    @MockitoBean CatalogQueryService catalog;
    @MockitoBean AccountQueryService accounts;

    @Test void anonymousCanReadProductDetailWithoutCsrf() throws Exception {
        var detail = new com.swp391.householdgoods.catalog.api.CatalogProductDetail(100, "Nồi", "Mô tả",
                1, "Nhà bếp", 1, "HomeMart", java.util.List.of("/cover.svg"), java.util.List.of());
        org.mockito.Mockito.when(catalog.productDetail(100)).thenReturn(detail);
        mvc.perform(get("/api/v1/catalog/products/100/detail"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productId").value(100))
                .andExpect(jsonPath("$.data.description").value("Mô tả"))
                .andExpect(jsonPath("$.data.images[0]").value("/cover.svg"));
        verifyNoInteractions(service);
    }

    @Test void detailReturns404ForHiddenProductsAnd400ForMalformedIds() throws Exception {
        org.mockito.Mockito.when(catalog.productDetail(100)).thenThrow(
                new com.swp391.householdgoods.catalog.application.CatalogException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "CATALOG_NOT_FOUND", "Không tìm thấy"));
        mvc.perform(get("/api/v1/catalog/products/100/detail")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/catalog/products/invalid/detail")).andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotWriteEvenWithCsrf() throws Exception {
        mvc.perform(post("/api/v1/products").with(csrf())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void customersAndStaffCannotCreateUpdateOrDelete() throws Exception {
        for (String role : new String[] {"CUSTOMER", "STAFF"}) {
            mvc.perform(post("/api/v1/products").with(user("test").roles(role)).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/products/123").with(user("test").roles(role)).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/products/123").with(user("test").roles(role)).with(csrf())).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void managerStillNeedsCsrfAndCanDeleteWithVersions() throws Exception {
        mvc.perform(delete("/api/v1/products/123").with(user("manager").roles("MANAGER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/products/123").param("productVersion","0").param("skuVersion","0")
                .with(user("manager").roles("MANAGER")).with(csrf())).andExpect(status().isNoContent());
        org.mockito.Mockito.verify(service).delete(123L,0L,0L);
    }
}
