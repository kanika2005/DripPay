package com.example.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Spring Boot application.
 *
 * @SpringBootApplication = three annotations combined:
 *   1. @Configuration  → this class can define Spring beans
 *   2. @EnableAutoConfiguration → Spring Boot wires common beans for us
 *   3. @ComponentScan → looks for @Controller/@Service/@Repository in this package and below
 *
 * Without this class, nothing starts. Spring needs one place to boot from.
 */
@SpringBootApplication
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }
}
