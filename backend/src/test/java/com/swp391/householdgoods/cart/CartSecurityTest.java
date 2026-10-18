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
    private static final java.util.UUID REQUEST_ID = java.util.UUID.randomUUID();
    @Autowired MockMvc mvc;
    @MockitoBean CartService service;
    @MockitoBean AccountQueryService accounts;
    @Test void guestsEmployeesAndRequestsWithoutCsrfCannotChangeCart() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        for (String role : new String[] {"ADMIN", "STAFF"}) {
            mvc.perform(get("/api/v1/cart").with(user("staff").roles(role))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER"))
                .header("Idempotency-Key", REQUEST_ID.toString()).contentType("application/json").content("{\"skuId\":1,\"quantity\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void fractionalZeroAndMissingQuantitiesAreRejectedBeforeSaving() throws Exception {
        for (String quantity : new String[] {"1.5", "0", "-1", "null", "2147483648"}) {
            mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                    .header("Idempotency-Key", REQUEST_ID.toString()).contentType("application/json").content("{\"skuId\":1,\"quantity\":"+quantity+"}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void usesSessionIdentityAndReturnsSavedCart() throws Exception {
        when(service.addItem("owner@example.com", 1L, 2, REQUEST_ID)).thenReturn(new CartService.Cart(java.util.List.of(), "0", false));
        mvc.perform(post("/api/v1/cart/items").with(user("owner@example.com").roles("CUSTOMER")).with(csrf())
                .header("Idempotency-Key", REQUEST_ID.toString()).contentType("application/json").content("{\"skuId\":1,\"quantity\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.subtotal").value("0"));
        verify(service).addItem("owner@example.com", 1L, 2, REQUEST_ID);
    }
    @Test void savingFailureIsReportedWithoutLeakingDatabaseDetails() throws Exception {
        when(service.addItem("customer", 1L, 2, REQUEST_ID)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("private database details"));
        mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                .header("Idempotency-Key", REQUEST_ID.toString()).contentType("application/json").content("{\"skuId\":1,\"quantity\":2}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CART_UNAVAILABLE"));
    }

    @Test void addRequiresARequestIdAndUpdateRequiresARevision() throws Exception {
        mvc.perform(post("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"skuId\":1,\"quantity\":2}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/cart/items/1").with(user("customer").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void staleUpdateReturnsConflictAndRemovalCanConfirmSuccessWithoutACartSnapshot() throws Exception {
        when(service.updateItem("customer", 1L, 2, REQUEST_ID)).thenThrow(
                new com.swp391.householdgoods.catalog.application.CatalogException(
                        org.springframework.http.HttpStatus.CONFLICT, "CART_CHANGED", "Reload"));
        mvc.perform(put("/api/v1/cart/items/1").with(user("customer").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"quantity\":2,\"expectedVersion\":\"" + REQUEST_ID + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CART_CHANGED"));
        when(service.remove("customer", java.util.List.of(1L)))
                .thenReturn(new CartService.RemovalResult(true, null, true, "Removed; reload"));
        mvc.perform(delete("/api/v1/cart/items").with(user("customer").roles("CUSTOMER")).with(csrf())
                .contentType("application/json").content("{\"skuIds\":[1]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.removed").value(true))
                .andExpect(jsonPath("$.data.refreshRequired").value(true));
    }
}
