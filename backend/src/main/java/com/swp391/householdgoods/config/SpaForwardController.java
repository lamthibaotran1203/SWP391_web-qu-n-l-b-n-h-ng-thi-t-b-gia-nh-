package com.swp391.householdgoods.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/account/password", "/manager/catalog", "/register", "/verify-email", "/staff/orders", "/products", "/products/{id}"})
    String forwardKnownClientRoutes() {
        return "forward:/index.html";
    }
}
