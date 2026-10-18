package com.swp391.householdgoods.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpaForwardControllerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin", "/staff", "/staff/orders",
            "/admin/catalog", "/admin/products", "/admin/products/new",
            "/admin/products/7", "/admin/products/7/edit",
            "/admin/products/7/skus/new", "/admin/products/7/skus/9/edit",
            "/admin/suppliers", "/admin/purchase-orders/create",
            "/account/password", "/account/profile", "/products/7"
    })
    void forwardsDirectClientLinksToTheReactEntryPoint(String path) throws Exception {
        MockMvcBuilders.standaloneSetup(new SpaForwardController()).build()
                .perform(get(path)).andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
