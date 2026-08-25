# ADR-000: Tech stack selection

- **Status:** Accepted
- **Date:** 2026-08-25
- **Deciders:** Ratin Sharma (stakeholder), Mavis (orchestrator)
- **Supersedes:** —
- **Superseded by:** —

## Context

The pack (`01_ARS.docx` §5) explicitly states that no specific
technology stack or vendor has been confirmed, and that "infrastructure
model first; technology stack and cloud capability are intentionally
deferred." That left us free to pick, but also obligated us to
document the choice before writing any code.

The product domain is a Fortune 500 insurance claims platform
(`ADR-012`), and the platform must run on physical servers, in four
sequential environments, with a central GenAI layer (`ADR-004`,
`ADR-005`).

We needed one coherent default stack that:

1. Is mature and supported long-term (regulated enterprise, 10-year
   horizon).
2. Has the largest hiring pool and the deepest pool of operators.
3. Plays well with the pack's hard constraints: on-prem, no cloud,
   no virtualization.
4. Supports the central, vendor-neutral GenAI requirement.
5. Supports the "24×7 logging, 24h retention" and "two DB instances,
   pipeline-mediated sync" requirements without exotic tooling.

## Decision

Adopt the following baseline. Any deviation requires a new ADR.

| Layer | Choice |
|-------|--------|
| Backend language | Java 21 (LTS) |
| Backend framework | Spring Boot 3.3 |
| Build (backend) | Maven 3.9 + Maven Wrapper |
| Database | PostgreSQL 16 |
| DB migration | Flyway 10 |
| App server | Spring Boot embedded Tomcat 10.1 |
| Frontend language | TypeScript 5.5+ |
| Frontend framework | React 18 (functional components only) |
| Frontend build | Vite 5 |
| Package manager | pnpm 9 |
| CI/CD | Jenkins 2.460 LTS, JCasC + Shared Library |
| Bare-metal provisioning | Ansible 9 |
| Firewall | nftables (kernel ≥ 5.15) |
| Reverse proxy / TLS | HAProxy 2.8 + Let's Encrypt / internal CA |
| Metrics | Prometheus 2.54 + Alertmanager 0.27 |
| Logs | Loki 3 + Promtail 3 (logback + logstash-logback-encoder) |
| Traces | OpenTelemetry Java agent + Tempo 2 |
| Dashboards | Grafana 11 |
| AuthN/AuthZ | Keycloak 25 (OIDC, realm export) |
| GenAI gateway | LiteLLM 1.4x |
| LLM provider | Configurable (OpenAI / Anthropic / Azure OpenAI / on-prem) |
| Vector store (RAG) | pgvector on the same Postgres 16 |
| Secrets (MVP) | Ansible Vault |
| Container build | Buildah + skopeo (rootless, no Docker daemon) |

## Consequences

**Positive**

- Every component above is open source with permissive licensing.
- Java 21 LTS runs through 2031; PostgreSQL 16 is supported through
  at least 2028 by the community; Spring Boot 3.x is the current GA
  line. The stack is genuinely 10-year safe.
- The hiring pool for Java + Spring + Postgres is the largest of any
  comparable stack, which matters for a Fortune 500 procurement.
- The "no cloud, no virtualization" constraint is satisfied without
  any awkward workarounds — every component runs on bare RHEL /
  Rocky / Debian.
- LiteLLM is the abstraction that lets the rest of the system be
  vendor-neutral against the LLM provider (see `ADR-005`,
  `ADR-011`).

**Negative / accepted cost**

- Spring Boot is a heavy framework. We pay the boot-time and memory
  cost in exchange for ecosystem maturity.
- Jenkins is famously the "easy to set up, hard to operate" CI.
  JCasC + Shared Library are the mitigations; if those are skipped,
  we will regret it.
- Grafana + Loki + Tempo + Prometheus is "yet another set of YAML
  files" to learn. The alternative was ELK + Jaeger, which is the
  same number of moving parts with worse query-language
  fragmentation.

**Neutral**

- The choice does not lock us in to a single LLM provider (ADR-011).
- The choice does not lock us in to a single hardware vendor.

## Alternatives considered

- **.NET 8 + SQL Server.** Strong in regulated US enterprise, but the
  PostgreSQL skill set is more portable, and Linux-only fits the
  on-prem constraint better.
- **Node.js 20 + TypeScript + NestJS + Postgres.** Lean, single
  language front + back, faster to MVP. Rejected because the hiring
  pool and operational tooling for JVM apps is deeper in Fortune 500
  shops.
- **GitHub Actions self-hosted / GitLab self-managed.** Rejected
  because the pack's "no cloud" is interpreted strictly; Jenkins
  on-prem is the safer default.

## References

- `01_ARS.docx` §2 (scope), §5 (open decisions)
- `ADR-001` (physical servers)
- `ADR-004` (central GenAI)
- `ADR-005` (vendor-neutral GenAI)
- `ADR-010` (no invented NFRs)
- `ADR-011` (GenAI data boundary)
