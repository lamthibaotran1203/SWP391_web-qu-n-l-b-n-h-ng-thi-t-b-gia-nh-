package com.swp391.householdgoods.cart;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
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

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, SecurityResponseHandlers.class, ApiProblemWriter.class})
class CartSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean CartService service;
    @MockitoBean AccountQueryService accounts;
    @Test void guestsEmployeesAndRequestsWithoutCsrfCannotChangeCart() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        for (String role : new String[] {"ADMIN", "STAFF"}) {
            mvc.perform(get("/api/v1/cart").with(user("staff").roles(role))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER"))
                .contentType("application/json").content("{\"skuId\":1,\"quantity\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void fractionalZeroAndMissingQuantitiesAreRejectedBeforeSaving() throws Exception {
        for (String quantity : new String[] {"1.5", "0", "-1", "null", "2147483648"}) {
            mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                    .contentType("application/json").content("{\"skuId\":1,\"quantity\":"+quantity+"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void usesSessionIdentityAndReturnsSavedCart() throws Exception {
        when(service.save("owner@example.com", 1L, 2, true)).thenReturn(new CartService.Cart(java.util.List.of(), "0", false));
        mvc.perform(post("/api/v1/cart/items").with(user("owner@example.com").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"skuId\":1,\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.subtotal").value("0"));
        verify(service).save("owner@example.com", 1L, 2, true);
    }
    @Test void savingFailureIsReportedWithoutLeakingDatabaseDetails() throws Exception {
        when(service.save("customer", 1L, 2, true)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("private database details"));
        mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"skuId\":1,\"quantity\":2}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CART_UNAVAILABLE"));
    }
}
