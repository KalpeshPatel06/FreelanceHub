package com.freelancehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FreelanceHub Application Entry Point
 *
 * @SpringBootApplication enables:
 *   - @ComponentScan      (finds all @Component, @Service, @Controller, etc.)
 *   - @EnableAutoConfiguration (configures Spring based on classpath)
 *   - @Configuration      (allows defining beans)
 */
@SpringBootApplication
public class FreelanceHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelanceHubApplication.class, args);
    }
}
