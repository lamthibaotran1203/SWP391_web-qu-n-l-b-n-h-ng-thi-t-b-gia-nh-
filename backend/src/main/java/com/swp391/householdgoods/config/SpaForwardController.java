package com.swp391.householdgoods.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/account/password",
            "/account/profile",
            "/register",
            "/verify-email",
            "/products",
            "/products/{id}",
            "/staff/orders",
            "/manager/catalog",
            "/manager/products",
            "/manager/products/new",
            "/manager/products/{productId}",
            "/manager/products/{productId}/edit",
            "/manager/products/{productId}/skus/new",
            "/manager/products/{productId}/skus/{skuId}/edit",
            "/manager/suppliers",
            "/manager/purchase-orders/create"
    })
    String forwardKnownClientRoutes() {
        return "forward:/index.html";
    }
}
