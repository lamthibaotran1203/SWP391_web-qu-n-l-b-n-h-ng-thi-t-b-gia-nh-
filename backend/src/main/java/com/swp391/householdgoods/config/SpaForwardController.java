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
            "/staff/products",
            "/staff/products/new",
            "/staff/products/{productId}",
            "/staff/products/{productId}/edit",
            "/staff/products/{productId}/skus/new",
            "/staff/products/{productId}/skus/{skuId}/edit",
            "/staff/suppliers",
            "/staff/purchase-orders/create"
    })
    String forwardKnownClientRoutes() {
        return "forward:/index.html";
    }
}
