package com.swp391.householdgoods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HouseholdGoodsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HouseholdGoodsApplication.class, args);
    }
}
