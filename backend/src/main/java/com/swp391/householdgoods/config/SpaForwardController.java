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
            "/staff",
            "/staff/orders",
            "/admin",
            "/admin/catalog",
            "/admin/products",
            "/admin/products/new",
            "/admin/products/{productId}",
            "/admin/products/{productId}/edit",
            "/admin/products/{productId}/skus/new",
            "/admin/products/{productId}/skus/{skuId}/edit",
            "/admin/brands",
            "/admin/suppliers",
            "/admin/purchase-orders/create"
    })
    String forwardKnownClientRoutes() {
        return "forward:/index.html";
    }
}
