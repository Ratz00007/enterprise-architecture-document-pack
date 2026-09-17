package com.acme.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Acme Claims — Spring Boot 3.3 on Java 21.
 *
 * <p>This is the entry point. Package layout follows the
 * ports-and-adapters pattern:
 *
 * <ul>
 *   <li>{@code domain} — pure business types (no Spring, no JPA)</li>
 *   <li>{@code application} — use cases / orchestration</li>
 *   <li>{@code infrastructure} — JPA repositories, external clients</li>
 *   <li>{@code api} — REST controllers, request/response DTOs</li>
 *   <li>{@code config} — Spring configuration</li>
 *   <li>{@code observability} — health, metrics, tracing glue</li>
 * </ul>
 */
@SpringBootApplication
public class ClaimsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsApplication.class, args);
    }
}
