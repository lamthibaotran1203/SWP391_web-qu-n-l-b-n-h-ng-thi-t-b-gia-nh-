package com.swp391.householdgoods.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/account/password",
            "/register",
            "/verify-email",
            "/products",
            "/products/{id}",
            "/staff/orders",
            "/manager/catalog",
            "/manager/suppliers",
            "/manager/purchase-orders/create"
    })
    String forwardKnownClientRoutes() {
        return "forward:/index.html";
    }
}
