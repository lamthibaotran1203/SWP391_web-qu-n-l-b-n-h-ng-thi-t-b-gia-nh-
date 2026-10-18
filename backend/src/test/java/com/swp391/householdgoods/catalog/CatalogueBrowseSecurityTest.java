package com.swp391.householdgoods.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.swp391.householdgoods.catalog.api.CatalogueBrowseController;
import com.swp391.householdgoods.catalog.application.CatalogueBrowseService;
import com.swp391.householdgoods.common.api.ApiProblemWriter;
import com.swp391.householdgoods.identity.application.AccountQueryService;
import com.swp391.householdgoods.security.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogueBrowseController.class)
@Import({SecurityConfig.class,SecurityResponseHandlers.class,ApiProblemWriter.class})
class CatalogueBrowseSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean CatalogueBrowseService service;
    @MockitoBean AccountQueryService accounts;
    @Test void publicBrowseAndCategoriesDoNotRequireAuthentication() throws Exception {
        when(service.browse(any())).thenReturn(new CatalogueBrowseService.Result(List.of(),0,12,0));
        when(service.categories(null,true)).thenReturn(List.of());
        mvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/v1/catalog/public-categories").param("roots","true")).andExpect(status().isOk());
    }
    @Test void rejectsUnsupportedAndRepeatedFilters() throws Exception {
        mvc.perform(get("/api/v1/catalog/products").param("unsupported","1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/products").param("categoryId","1","2")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalog/products").param("categoryId","bad")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void temporaryFailuresReturnSafeRetryableErrors() throws Exception {
        when(service.browse(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database info"));
        mvc.perform(get("/api/v1/catalog/products")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CATALOG_UNAVAILABLE"));
    }
}
