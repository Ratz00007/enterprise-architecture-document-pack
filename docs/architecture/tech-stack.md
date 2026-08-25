# Tech Stack

> Every confirmed technology choice, with the reason, the cost, and the
> place to change it. If something is not in this document, it has not
> been decided.

## Stack at a glance

| Layer | Choice | Version | ADR | License | Cost-of-change |
|-------|--------|---------|-----|---------|----------------|
| Language (backend) | Java | 21 LTS | ADR-000 | OpenJDK (GPLv2+CPE) | Low — language-agnostic contracts |
| Framework (backend) | Spring Boot | 3.3.x | ADR-000 | Apache 2.0 | Medium — heavy framework lock-in |
| Build (backend) | Maven | 3.9.x | ADR-000 | Apache 2.0 | Low |
| Language (frontend) | TypeScript | 5.5+ | ADR-000 | Apache 2.0 | Low |
| UI framework | React | 18.3+ | ADR-000 | MIT | Medium |
| Build (frontend) | Vite | 5.x | ADR-000 | MIT | Low |
| Package manager | pnpm | 9.x | ADR-000 | MIT | Low |
| Database | PostgreSQL | 16.x | ADR-000 | PostgreSQL | Medium — schema-coupled |
| ORM / migration | Flyway | 10.x | ADR-000 | Apache 2.0 | Low |
| Driver | pgjdbc | 42.7+ | ADR-000 | BSD-2 | Low |
| App server | Spring Boot embedded Tomcat | 10.1.x | ADR-000 | Apache 2.0 | Low |
| AuthN/AuthZ | Keycloak | 25.x | ADR-000 | Apache 2.0 | Medium |
| CI/CD | Jenkins | 2.460.x LTS | ADR-000 | MIT | High — pipeline-coupled |
| Pipeline-as-code | Jenkins Shared Library + JCasC | n/a | ADR-000 | MIT | Medium |
| Container build | Buildah + skopeo (no Docker daemon) | latest | ADR-000 | Apache 2.0 | Low |
| IaC (bare metal) | Ansible | 9.x | ADR-000 | GPLv3 | Low |
| Firewall | nftables | kernel ≥ 5.15 | ADR-000 | GPLv2 | Medium |
| Reverse proxy / TLS | HAProxy + Let's Encrypt / internal CA | latest | ADR-000 | GPLv2 / various | Medium |
| Metrics | Prometheus | 2.54.x | ADR-000 | Apache 2.0 | Medium |
| Dashboards | Grafana | 11.x | ADR-000 | AGPLv3 (OSS) | Medium |
| Logs (store) | Loki | 3.x | ADR-000 | AGPLv3 (OSS) | Medium |
| Logs (ship) | Promtail | 3.x | ADR-000 | AGPLv3 (OSS) | Low |
| Logs (agent lib) | Logback + logstash-logback-encoder | 1.5 / 7.4 | ADR-000 | EPL / MIT | Low |
| Traces | OpenTelemetry Java agent + Tempo | 1.42+ / 2.x | ADR-000 | Apache 2.0 | Low |
| Alerts | Alertmanager | 0.27.x | ADR-000 | Apache 2.0 | Low |
| Secrets | Ansible Vault (MVP) → HashiCorp Vault (post-MVP) | n/a | ADR-000 | MPL / BUSL | Medium |
| GenAI gateway | LiteLLM | 1.4x | ADR-011 | MIT | Low — abstraction layer |
| LLM provider | Configurable (OpenAI / Anthropic / Azure OpenAI / on-prem) | n/a | ADR-011 | n/a | Provider swap, low code impact |
| Vector store (RAG) | pgvector (Postgres 16 ext) | 0.7+ | ADR-011 | PostgreSQL | Low |
| Unit testing | JUnit 5 + AssertJ + Mockito | latest | ADR-013 | EPL / Apache 2.0 / MIT | Low |
| Integration testing | Testcontainers (Java) | 1.20+ | ADR-013 | MIT | Low |
| Contract testing | Pact (provider-driven) | 4.x | ADR-013 | Apache 2.0 | Low |
| E2E UI testing | Playwright | 1.4x | ADR-013 | Apache 2.0 | Low |
| BDD | Cucumber | 7.x | ADR-013 | MIT | Low |
| Load testing | k6 | 0.50+ | ADR-013 | AGPLv3 | Low |
| SAST | SpotBugs + Checkstyle + PMD | latest | ADR-000 | EPL / LGPL / BSD | Low |
| Dependency CVE | OWASP Dependency-Check + Trivy | latest | ADR-000 | Apache 2.0 | Low |
| DAST | OWASP ZAP baseline | latest | ADR-000 | Apache 2.0 | Low |
| License compliance | license-maven-plugin | latest | ADR-000 | Apache 2.0 | Low |

## What we explicitly do **not** use

- **No Kubernetes.** The pack says "physical servers only, no
  virtualization". Even container orchestration layers count as
  virtualization for our purposes. (See ADR-001.)
- **No cloud services.** No AWS, Azure, GCP, or anything that phones home.
  Everything is on-prem. (See ADR-001.)
- **No Citrix, no VDI.** Confirmed non-requirement.
- **No Hibernate-isms on the public API surface.** Repositories return
  domain types, never entities. (See ADR-013.)
- **No Lombok.** Records and explicit constructors are preferred for
  clarity and to keep debug traces readable. (See ADR-013.)
- **No `any` in TypeScript.** ESLint rule `@typescript-eslint/no-explicit-any`
  is set to `error`. (See ADR-013.)

## Why Java 21 / Spring Boot 3.3

- **Most common at Fortune 500 regulated enterprises.** Hiring pool is
  the largest of any backend stack; long-term support is real (Java 21
  is LTS through at least 2031).
- **Records, sealed types, virtual threads** (in preview) make modern
  Java genuinely modern, not 2010-Java-in-a-suit.
- **Spring Boot 3.3** runs on Jakarta EE 10, supports GraalVM native
  build, has first-class observability, validation, and security
  integrations.
- **Maven** over Gradle: more enterprise-mature, easier to find
  consultants for, more reproducible builds out of the box.

## Why PostgreSQL 16

- **Most advanced open-source OLTP database.** MVCC, partial indexes,
  generated columns, JSONB, range types, partitioning, strong
  consistency.
- **pgvector extension** lets us run the GenAI RAG store in the same
  engine as the operational data — fewer moving parts, one backup
  story. (See ADR-011.)
- **Mature JDBC driver, mature Flyway support**, mature on-prem
  operations tooling (pgBackRest, Barman, pg_stat_statements).
- **Familiar to every DBA we will hire** for a Fortune 500 environment.

## Why Jenkins (not GitHub Actions / GitLab CI)

- **The pack says "no cloud"**. GitHub Actions and GitLab SaaS are out.
  Self-hosted GitHub Actions runners and self-managed GitLab are
  possible, but Jenkins is the established on-prem default at the kind
  of company we're building for.
- **Jenkins Shared Library** lets us codify the four-stage promotion
  pattern once and reuse it across every microservice we add.
- **JCasC (Configuration as Code)** means the entire Jenkins
  configuration is in this repo, not in a snowflake UI.

## Why Prometheus + Grafana + Loki (not ELK)

- **Single operational story.** Same query language (LogQL/ PromQL),
  same alerting path (Alertmanager), same dashboard tool (Grafana).
- **Pull-based metrics** match the "firewall + IP allow-list" baseline
  better than push-based collectors.
- **Loki's label-based log model** is dramatically cheaper to operate
  at our scale than an Elasticsearch cluster.

## Why Keycloak (not Auth0, Cognito, Okta)

- **Self-hosted, on-prem, vendor-neutral, no per-MAU pricing.**
  Critical when you cannot phone home.
- **OIDC + SAML + LDAP federation** in one box. Plays well with
  enterprise AD.
- **Realm export** lets us version-control auth configuration
  (`services/auth-realm/`).

## When to revisit a stack choice

- **Every 6 months** at the architecture review (see `adr/INDEX.md`).
- **Immediately** if any of: a CVE with no patch path, a vendor EOL
  announcement, a major shift in our NFRs (the pack deliberately
  leaves these open), a hiring crisis.
- Any change goes through a new ADR; never edit an existing ADR
  retroactively.
