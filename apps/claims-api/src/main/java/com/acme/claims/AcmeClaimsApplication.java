package com.acme.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Acme Claims API - FNOL, triage, adjudication and payout for the on-premises
 * Acme Claims platform (Dev → QA → UAT → Prod, ADR-002).
 */
@SpringBootApplication
@EnableFeignClients
public class AcmeClaimsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeClaimsApplication.class, args);
    }
}
