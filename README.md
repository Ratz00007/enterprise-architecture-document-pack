# Acme Claims

> **Insurance claims processing — production-grade — built end-to-end against
> the enterprise on-premises architecture pack.**

[![status: foundation](https://img.shields.io/badge/status-foundation-blue)](#)
[![stack: Java 21 + Spring Boot 3.3 + PostgreSQL 16](https://img.shields.io/badge/stack-Java%2021%20%2B%20Spring%20Boot%203.3%20%2B%20PostgreSQL%2016-blue)](#)
[![environments: Dev → QA → UAT → Prod](https://img.shields.io/badge/environments-Dev%20%E2%86%92%20QA%20%E2%86%92%20UAT%20%E2%86%92%20Prod-blue)](#)
[![GenAI: vendor-neutral, LiteLLM](https://img.shields.io/badge/GenAI-vendor--neutral%20via%20LiteLLM-blue)](#)

---

## What this is

A realistic, Fortune 500–grade insurance claims platform, **plus** the
on-premises DevOps + central GenAI platform that hosts it. Everything
documented in the architecture pack at the root of this repository has
been turned into a runnable, traceable system.

| | |
|--|--|
| **Domain** | Insurance claims (FNOL → triage → adjudication → payout) |
| **Hosting** | On-premises, physical servers, no cloud, no virtualization, no VDI |
| **Environments** | Dev → QA → UAT → Production (sequential, human-gated) |
| **Network** | Same VLAN/subnet, logical separation via firewall + IP + port |
| **Databases** | Two instances (QA, Prod) with pipeline-mediated Prod→QA image refresh |
| **GenAI** | Central, vendor-neutral gateway (LiteLLM) — advisory only, humans approve |
| **Stack** | Java 21 · Spring Boot 3.3 · PostgreSQL 16 · React 18 · TypeScript · Ansible · Jenkins · Prometheus · Grafana · Loki · Keycloak |
| **Observability** | 24×7 structured logs (24h retention/reset) + separate issue monitoring |
| **Security baseline** | Firewall, IP allow-list, port allow-list, OIDC via Keycloak, secrets in Ansible Vault |

## Read this first

1. **[`AGENTS.md`](./AGENTS.md)** — the agent & engineer contract for this repo.
2. **[`docs/architecture/system-context.md`](./docs/architecture/system-context.md)** — one-page system overview.
3. **[`docs/architecture/tech-stack.md`](./docs/architecture/tech-stack.md)** — every component, why we picked it, what it costs us.
4. **[`docs/architecture/multi-agent-plan.md`](./docs/architecture/multi-agent-plan.md)** — who is building what, right now.
5. **[`docs/architecture/adr/INDEX.md`](./docs/architecture/adr/INDEX.md)** — every architecture decision, in one place.

Then the source-of-truth pack:

- `00_Document_Control_Map.docx`
- `01_ARS.docx` (requirements baseline)
- `02_HLA.docx` (high-level architecture)
- `03_DINA.docx` (detailed infrastructure & network)
- `04_CICD_DevOps.docx`
- `05_GenAI_Architecture.docx`
- `06_Data_Architecture.docx`
- `07_Test_Strategy.docx`
- `08_Observability.docx`
- `09_Security_Network.docx`
- `10_RTM_ADR.docx` (traceability + ADRs)
- `11_Operations_Runbooks.docx`

## Quick start

```powershell
# 1. Bootstrap the toolchain
pwsh -File scripts\setup-dev.ps1

# 2. Start the API
cd apps\claims-api
.\mvnw.cmd spring-boot:run

# 3. Start the web app (in a second terminal)
cd apps\claims-web
pnpm install
pnpm dev

# 4. Open the app
# Web:  http://localhost:5173
# API:  http://localhost:8080/actuator/health
```

## Repository map

| Path | What lives here |
|------|-----------------|
| `apps/claims-api/` | Spring Boot 3.3 product API (Java 21) |
| `apps/claims-web/` | React 18 + TypeScript adjuster & customer UI (Vite) |
| `services/` | Platform services: GenAI gateway, DB sync, audit log, auth realm, log aggregator |
| `infra/` | Bare-metal provisioning: Ansible, firewall, monitoring, systemd |
| `ci/` | Jenkins pipeline, shared library, promotion scripts |
| `docs/architecture/` | System context, stack, topology, pipeline, observability, security, **ADRs** |
| `docs/domain/` | Claims domain: entities, workflows, regulatory context |
| `docs/api/` | OpenAPI specs, event schemas |
| `data/` | DB schemas, seed data, masking rules |
| `tests/` | E2E, contract, load, fixtures |
| `ops/` | Dashboards, incident response, oncall rotation |
| `scripts/` | Local-dev, lint, ADR generation |

## Status

| Workstream | Status | Owner |
|------------|--------|-------|
| Architecture & ADRs | ✅ Foundation complete; register reconciled (ADR-006..013 written, ADR-021..024 added) | Architect lead |
| App API skeleton (Spring Boot) | ✅ Core domain + REST implemented (FNOL→CLOSED, idempotency, OIDC); contract/E2E suites pending | Backend lead |
| Web skeleton (React + TS) | 🟡 `package.json` only, no UI yet | Frontend lead |
| GenAI gateway | 🟡 Client scaffolded in claims-api; gateway service itself not started | Platform lead |
| DB sync pipeline | 🟡 Scaffolded, mechanism TBD | Data lead |
| Observability | 🟡 Prometheus + Loki config drafted | SRE lead |
| Security baseline | 🟡 OIDC resource server live in API; realm config not in repo | Security lead |
| Test strategy | 🟡 Unit suite green (54 tests, 98%/95% cov); Testcontainers suite needs Docker; contract/E2E pending | QA lead |

## License

Internal — see `LICENSE` (TBD by Legal).
