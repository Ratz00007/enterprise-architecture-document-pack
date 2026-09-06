# Architecture Decision Register — Index

> Every decision that shapes this system, in one place. Pack-derived
> ADRs (ADR-001 through ADR-010) are transcribed from
> `10_RTM_ADR.docx` §2; project-introduced ADRs (ADR-000, 011, 012,
> 013, …) are the ones we add to actually implement the pack.

## How to use this index

- **New here?** Skim the "Status" column. Anything in **Proposed** is
  up for debate; **Accepted** is locked until superseded.
- **About to make a non-trivial choice?** Read the matching ADR first.
  If none fits, write a new one with `pwsh -File scripts\gen-adr.ps1`
  before writing code.
- **Disagree with a decision?** Open a PR that supersedes the ADR.
  Never edit a closed ADR retroactively.

## Register

| # | Title | Status | Origin | Affects |
|---|-------|--------|--------|---------|
| [ADR-000](./ADR-000-tech-stack.md) | Tech stack selection: Java 21 / Spring Boot 3.3 / PostgreSQL 16 / React 18 / Jenkins / Prometheus / LiteLLM / Keycloak | **Accepted** | This project (locked in by stakeholder) | All components |
| [ADR-001](./ADR-001-physical-servers-only.md) | Physical servers only, no virtualization, no cloud | **Accepted** | Pack ADR-001 | All infrastructure |
| [ADR-002](./ADR-002-four-sequential-environments.md) | Four sequential environments: Dev → QA → UAT → Prod | **Accepted** | Pack ADR-002 | Promotion, CI/CD |
| [ADR-003](./ADR-003-same-vlan-subnet.md) | Same VLAN / subnet, logical separation only | **Accepted** (with security review) | Pack ADR-003 | Network, security |
| [ADR-004](./ADR-004-central-genai.md) | Central GenAI as the cross-environment intelligence layer | **Accepted** | Pack ADR-004 | All environments |
| [ADR-005](./ADR-005-vendor-agnostic-genai.md) | Vendor-agnostic GenAI: provider abstraction required | **Accepted** | Pack ADR-005 | GenAI integration |
| [ADR-006](./ADR-006-human-approval.md) | Human approval mandatory for every promotion; no autonomous prod deploys | **Accepted** | Pack ADR-006 | Promotion, governance |
| [ADR-007](./ADR-007-two-db-instances.md) | Two database instances only: QA + Production | **Accepted** | Pack ADR-007 | Data architecture |
| [ADR-008](./ADR-008-prod-image-to-qa.md) | Production image replicated to QA via controlled pipeline | **Accepted** (mechanism TBD) | Pack ADR-008 | Data architecture, CI/CD |
| [ADR-009](./ADR-009-24h-log-retention.md) | 24×7 logging with 24-hour retention/reset | **Accepted** | Pack ADR-009 | Observability |
| [ADR-010](./ADR-010-no-advanced-nfrs.md) | No invented NFRs (performance, HA, advanced security) | **Accepted** | Pack ADR-010 | Everything — guards against scope creep |
| [ADR-011](./ADR-011-genai-data-boundary-and-gateway.md) | GenAI gateway via LiteLLM; production data is allow-listed, never default | **Accepted** | This project | GenAI integration, data architecture |
| [ADR-012](./ADR-012-insurance-claims-domain-scope.md) | Application domain: insurance claims processing (FNOL → adjudication → payout) | **Accepted** | This project (locked in by stakeholder) | Product scope |
| [ADR-013](./ADR-013-test-pyramid-and-quality-gates.md) | Test pyramid, coverage gates, and required promotion checks | **Accepted** | This project | Test strategy, CI/CD |
| [ADR-014](./ADR-014-physical-server-specifications.md) | Physical server specifications, capacity, and NIC configurations | **Accepted** | This project | Infrastructure, procurement |
| [ADR-015](./ADR-015-backup-dr-bcp.md) | Backup, disaster recovery, and business continuity baseline | **Accepted** | This project | Operations, compliance |
| [ADR-016](./ADR-016-identity-authn-authz.md) | Identity, authentication, and authorization beyond OIDC+RBAC | **Accepted** | This project | Security, Keycloak |
| [ADR-017](./ADR-017-data-masking-sanitization.md) | Production data masking and sanitization for two-database sync | **Accepted** | This project | Data architecture, security |
| [ADR-018](./ADR-018-manual-testing-subtypes.md) | Manual testing sub-types: UAT and Operational Readiness Testing | **Accepted** | This project | Test strategy, quality |
| [ADR-019](./ADR-019-observability-tooling.md) | Observability tooling: Prometheus + Grafana + Loki + Tempo | **Accepted** | This project | Observability, operations |
| [ADR-020](./ADR-020-ha-dr-targets.md) | High availability and disaster recovery targets (baseline, RPO/RTO TBD) | **Accepted** (Baseline - Targets TBD) | This project | Architecture, operations |
| [ADR-021](./ADR-021-claims-lifecycle-state-machine.md) | Claim lifecycle state machine (FNOL → … → CLOSED, rejection path) | **Accepted** | This project | Claims API, web, testing |
| [ADR-022](./ADR-022-money-minor-units.md) | Money as BIGINT minor units (single currency for v1) | **Accepted** | This project | Claims API, data architecture, web |
| [ADR-023](./ADR-023-idempotency-keys.md) | Idempotency keys via an idempotency_keys table | **Accepted** | This project | Claims API, web, CI/CD |
| [ADR-024](./ADR-024-uuidv7-application-side.md) | UUIDv7 primary keys, generated application-side | **Accepted** | This project | Data architecture, all services |

## Proposed (open for review)

_None at this revision._

## Superseded

_None at this revision._

## Deferred (out of current scope, but tracked)

These are explicitly mentioned in the pack as "deferred" or
"not yet specified". They will get their own ADR when stakeholder
approves opening the topic.

| Topic | Pack reference | Notes |
|-------|----------------|-------|
| Physical server specifications, capacity, NICs | `00_Document_Control_Map.docx` §3 | Captured as ADR-014 (TBD) once we get the hardware quote |
| Backup / DR / business continuity / retention | `01_ARS.docx` §5 | Captured as ADR-015 (TBD) |
| Identity / authn / authz depth beyond OIDC + RBAC | `09_Security_Network.docx` §3 | Captured as ADR-016 (TBD) |
| Production data masking / sanitization rules | `06_Data_Architecture.docx` §3 | Captured as ADR-017 (TBD) — masking rules drafted in `data/masking/` |
| Exact firewall product / existing enterprise firewall | `01_ARS.docx` §5 | nftables is the default; can be repackaged for `pfSense` / `OPNsense` if stakeholder chooses |
| Exact GenAI provider / hosting location | `01_ARS.docx` §5 | LiteLLM is the gateway; provider is config-driven (ADR-011) |
| Manual test sub-types (terminology TBD) | `01_ARS.docx` §5 | Captured as ADR-018 (TBD) once stakeholder confirms the two sub-types |
| UAT user access path (direct vs app access layer) | `01_ARS.docx` §5 | TBD; default = app access layer through HAProxy |
| Whether prod receives direct inbound GenAI traffic | `01_ARS.docx` §5 | Default = no; only metadata/telemetry outbound (ADR-011) |
| Exact observability tooling (now Prometheus+Grafana+Loki) | `01_ARS.docx` §5 | Default chosen in ADR-000; ADR-019 (TBD) for any swap |
| HA / DR targets (RPO, RTO) | `01_ARS.docx` §4 | **Not invented** per ADR-010. Captured as ADR-020 (TBD) when stakeholder supplies targets |
