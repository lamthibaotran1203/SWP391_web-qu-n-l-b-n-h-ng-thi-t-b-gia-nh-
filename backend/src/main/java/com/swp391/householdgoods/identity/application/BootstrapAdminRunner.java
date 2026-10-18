package com.swp391.householdgoods.identity.application;

import com.swp391.householdgoods.config.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final BootstrapAdminService service;

    public BootstrapAdminRunner(
            BootstrapAdminProperties properties,
            BootstrapAdminService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isEmpty()) {
            log.info("Bootstrap Admin is not configured; no account was created");
            return;
        }

        BootstrapAdminResult result = service.initialize(properties);
        switch (result) {
            case CREATED -> log.info("Bootstrap Admin account created");
            case UPDATED -> log.info("Bootstrap Admin display name updated");
            case UNCHANGED -> log.info("Bootstrap Admin account already exists");
        }
    }
}
