package com.regression;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot application for HexaGlue JPA regression tests.
 *
 * <p>This application validates that generated JPA infrastructure
 * works correctly with a real database (H2 in-memory).
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.regression.infrastructure.persistence")
public class RegressionTestsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegressionTestsApplication.class, args);
    }
}
