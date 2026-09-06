package com.acme.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Acme Claims Processing Platform - Main Application Entry Point
 * 
 * Fortune 500-grade insurance claims processing platform handling:
 * - First Notice of Loss (FNOL)
 * - Triage and Assessment
 * - Adjudication
 * - Payout Processing
 * 
 * Architecture Constraints:
 * - Physical servers only (no cloud/virtualization)
 * - Four sequential environments: Dev → QA → UAT → Prod
 * - Same VLAN/subnet networking with firewall controls
 * - Central GenAI gateway integration
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties
public class AcmeClaimsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeClaimsApplication.class, args);
    }
}
