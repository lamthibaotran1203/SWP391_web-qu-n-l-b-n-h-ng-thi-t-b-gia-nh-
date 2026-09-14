package com.swp391.householdgoods.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String fromEmail, String fromName) {
}
