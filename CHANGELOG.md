# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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
