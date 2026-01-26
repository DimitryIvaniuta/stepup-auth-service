package com.github.dimitryivaniuta.gateway.stepupauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Step-Up Authentication (risk-based) service.
 */
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
public class StepUpAuthApplication {
    /** Boots the application. */
    public static void main(String[] args) {
        SpringApplication.run(StepUpAuthApplication.class, args);
    }
}
