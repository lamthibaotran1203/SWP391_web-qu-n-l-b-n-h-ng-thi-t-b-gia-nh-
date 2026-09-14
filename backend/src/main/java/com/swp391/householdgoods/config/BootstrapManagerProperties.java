package com.swp391.householdgoods.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-manager")
public record BootstrapManagerProperties(String email, String password, String fullName) {

    public boolean isEmpty() {
        return isBlank(email) && isBlank(password) && isBlank(fullName);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
