package com.swp391.householdgoods;

import com.swp391.householdgoods.config.SchemaMigrationCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class HouseholdGoodsApplication {

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--seed-catalog-demo")) {
            System.exit(
                    com.swp391.householdgoods.catalog.application
                            .CatalogDemoCommand.execute());
            return;
        }

        if (SchemaMigrationCommand.handles(args)) {
            System.exit(
                    SchemaMigrationCommand.execute(
                            java.util.Arrays.asList(args)
                                    .contains("--migrate-schema")));
            return;
        }

        SpringApplication.run(
                HouseholdGoodsApplication.class,
                args);
    }
}