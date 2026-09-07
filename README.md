# Acme Claims

> **Insurance claims processing — production-grade — built end-to-end against
> the enterprise on-premises architecture pack.**

[![stack: Java 21 + Spring Boot 3.3 + PostgreSQL 16](https://img.shields.io/badge/stack-Java%2021%20%2B%20Spring%20Boot%203.3%20%2B%20PostgreSQL%2016-blue)](#)
[![environments: Dev → QA → UAT → Prod](https://img.shields.io/badge/environments-Dev%20%E2%86%92%20QA%20%E2%86%92%20UAT%20%E2%86%92%20Prod-blue)](#)
[![GenAI: vendor-neutral, LiteLLM](https://img.shields.io/badge/GenAI-vendor--neutral%20via%20LiteLLM-blue)](#)
[![tests: 54 unit · Testcontainers · Playwright · k6](https://img.shields.io/badge/tests-unit%20%C2%B7%20Testcontainers%20%C2%B7%20Playwright%20%C2%B7%20k6-blue)](#)

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
3. **[`docs/architecture/adr/INDEX.md`](./docs/architecture/adr/INDEX.md)** — every architecture decision (ADR-000..024).
4. **[`docs/api/claims-api-v1.yaml`](./docs/api/claims-api-v1.yaml)** — the API contract (OpenAPI).

Then the source-of-truth pack: `00_Document_Control_Map.docx` …
`11_Operations_Runbooks.docx` (requirements → runbooks).

## Quick start (one command)

```powershell
pwsh -File scripts\setup-dev.ps1   # checks the toolchain, prints this plan
docker compose up --build          # the whole platform, one origin
```

| URL | What |
|-----|------|
| http://localhost:8081 | Web UI (sign in: `adjuster1` / `adjuster-dev-only`) |
| http://localhost:8080/api/v1/actuator/health | Claims API |
| http://localhost:8180 | Keycloak admin (`admin` / `admin-dev-only`) |
| http://localhost:3000 | Grafana (provisioned Acme Claims API dashboard) |
| http://localhost:9000 | LiteLLM GenAI gateway (dev sandbox provider) |

One origin serves the UI, the API (`/api/…`), Keycloak (`/realms/…`) and the
GenAI gateway (`/genai/…`), so the OIDC issuer is consistent everywhere.

**Run a claim end-to-end:** sign in → *Report FNOL* → submit → *Start triage*
→ *Begin adjudication* → *Approve* → *Initiate payout* → *Complete payout* →
*Close claim*.

## Running the pieces individually

```bash
# API (needs PostgreSQL on :5432; schema auto-applies only via compose —
# otherwise: bash data/scripts/apply-schema.sh)
cd apps/claims-api && ./mvnw spring-boot:run

# Web with Vite dev server (proxies /api and /realms to localhost)
cd apps/claims-web && npm install && npm run dev
```

## Tests

| Suite | Command | Needs |
|-------|---------|-------|
| API unit + integration | `cd apps/claims-api && ./mvnw verify` | Docker for Testcontainers |
| Web unit | `cd apps/claims-web && npm test` | Node 20+ |
| E2E (adjuster journey) | `cd tests/e2e && npx playwright test` | compose stack running |
| Load (100 RPS, p95<400ms gate) | `k6 run tests/load/claims-lifecycle.js` | k6 |
| Contract (consumer) | `cd tests/contract/consumer && npm run test:contract` | Node 20+ |
| Prod→QA sync dry run | `services/db-sync/sync-prod-to-qa.sh --prod-host … --qa-host …` | psql, jq |

## Repository map

| Path | What lives here |
|------|-----------------|
| `apps/claims-api/` | Spring Boot 3.3 product API (Java 21): lifecycle state machine, idempotent REST, OIDC |
| `apps/claims-web/` | React 18 + TypeScript adjuster UI (Vite): FNOL intake, claims list/detail, transitions |
| `services/auth-realm/` | Keycloak realm export (roles, clients, dev users) |
| `services/genai-gateway/` | LiteLLM config + sandbox provider with the data-classification policy |
| `services/db-sync/` | Prod→QA image refresh pipeline (masking, validation, provenance) |
| `infra/ansible/` | Bare-metal provisioning: baseline, firewall (nftables), API servers, monitoring |
| `infra/systemd/` | claims-api service unit |
| `infra/monitoring/` | Prometheus, Loki (24h retention), Promtail, Grafana provisioning |
| `ci/` | Jenkins pipeline (dev→qa→uat→prod with human gates) + `scripts/promote.ps1` |
| `docs/architecture/` | System context, stack, topology, pipeline, observability, security, **ADRs** |
| `docs/api/` | OpenAPI spec for the Claims API |
| `data/` | DB schemas, masking rules, schema-apply script |
| `tests/` | E2E (Playwright), load (k6), contract (Pact consumer) |
| `ops/` | Runbooks (incident response) |
| `scripts/` | `setup-dev.ps1`, local-dev helpers |

## Status

| Workstream | Status | Owner |
|------------|--------|-------|
| Architecture & ADRs | ✅ Foundation complete; register ADR-000..024 reconciled | Architect lead |
| Claims API (Spring Boot) | ✅ Core domain + REST implemented; 54 unit tests, 98/95% coverage, Testcontainers suite | Backend lead |
| Web UI (React + TS) | ✅ FNOL intake, claims list/detail, lifecycle transitions; vitest suite | Frontend lead |
| GenAI gateway | ✅ LiteLLM + sandbox provider with data-classification policy (dev); real provider config pending | Platform lead |
| DB sync pipeline | ✅ Prod→QA refresh script (masking + validation + provenance); scheduler TBD | Data lead |
| Observability | ✅ Prometheus/Loki(24h)/Grafana provisioned in compose; alerting rules pending | SRE lead |
| Security baseline | ✅ OIDC resource server + realm export + nftables baseline; prod secrets via Vault | Security lead |
| Test strategy | ✅ Unit + Testcontainers + E2E + k6 + Pact consumer; **Pact provider verification pending** | QA lead |

**Known gaps before the first QA cut:** Pact provider verification on the
promotion path, Grafana alert rules, DB-sync scheduler (Jenkins job), Keycloak
realm overlay per environment (secrets via Vault, no dev users).

## License

Internal — see `LICENSE` (TBD by Legal).
