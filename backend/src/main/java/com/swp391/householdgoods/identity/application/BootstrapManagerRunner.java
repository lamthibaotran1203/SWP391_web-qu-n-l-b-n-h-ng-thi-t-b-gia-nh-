package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.config.BootstrapManagerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapManagerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapManagerRunner.class);

    private final BootstrapManagerProperties properties;
    private final BootstrapManagerService service;

    public BootstrapManagerRunner(
            BootstrapManagerProperties properties,
            BootstrapManagerService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isEmpty()) {
            log.info("Bootstrap Manager is not configured; no account was created");
            return;
        }

        boolean created = service.initialize(properties);
        log.info(created ? "Bootstrap Manager account created" : "Bootstrap Manager account already exists");
    }
}
