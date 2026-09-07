# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Claims API core (v0.2.0-draft, PR #2):** claim lifecycle state machine
  (ADR-021), idempotent REST API (ADR-023), BIGINT minor-unit money (ADR-022),
  UUIDv7 PKs (ADR-024), OIDC resource server, PostgreSQL schema rewritten to
  the AGENTS.md rules, 54 unit tests at 98%/95% coverage, ADR register
  reconciled (ADR-006..013 written, ADR-021..024 added).
- **Platform build-out (PR #3):** Keycloak realm export
  (`services/auth-realm/`), LiteLLM GenAI gateway with sandbox provider and
  data-classification policy (`services/genai-gateway/`), Prod→QA sync
  pipeline with masking + provenance (`services/db-sync/`),
  Prometheus/Loki(24h)/Grafana stack (`infra/monitoring/`), Ansible
  provisioning + nftables + systemd (`infra/`), claims-web React UI (FNOL →
  CLOSED workflow), docker-compose full stack with same-origin nginx,
  Playwright E2E, k6 load profile, Pact consumer contract, OpenAPI spec,
  Maven wrapper, `scripts/setup-dev.ps1`, `ci/scripts/promote.ps1`.
- **Foundation push (v0.1.0-draft):** repository scaffold, full monorepo
  layout, 13 ADRs (10 from pack + 3 new), domain docs, infra skeletons,
  Spring Boot API skeleton, React web skeleton, Prometheus/Grafana/Loki
  config, Jenkins pipeline + JCasC, scripts.
- `AGENTS.md` — root agent & engineer contract.
- `docs/architecture/multi-agent-plan.md` — orchestration plan for the
  build-out workstreams.

### Changed
- n/a

### Deprecated
- n/a

### Removed
- n/a

### Fixed
- n/a

### Security
- n/a

---

## Versioning notes

- **0.x.y** — pre-MVP, breaking changes allowed at minor boundaries.
- **1.0.0** — first production cutover to UAT, then Production.
- **1.x.y** — production releases; only additive changes on `main` until
  the next minor. Bug fixes back-ported to supported releases.
