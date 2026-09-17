# Multi-Agent Workstream Plan

> The active build-out plan, the way a senior engineering manager
> would write it: who owns what, what the acceptance criteria are,
> what the hand-offs look like, and what blocks what. Updated
> whenever a workstream starts, finishes, or changes scope.

## Operating model

We run as one team with named workstreams. Each workstream has:

- An **owner** (the agent that drives the day-to-day work in that
  workstream).
- A **scope** (the files / components they may change without
  asking).
- A **definition of done** (what must be true to call the
  workstream complete).
- A **hand-off contract** (what they deliver to the next
  workstream).

Workstreams do not overlap on files. When they need to share code,
the upstream workstream ships first and the downstream workstream
branches from there.

## Workstreams

| # | Workstream | Owner agent | Scope | Status | Target |
|---|------------|-------------|-------|--------|--------|
| 1 | Architecture & ADR ledger | `architect` | `docs/architecture/`, `AGENTS.md`, `README.md` | **Done (foundation)** | Continuous maintenance |
| 2 | Domain model (claims) | `architect` + `backend` | `docs/domain/` | **Done (foundation)**, refine in build-out | v0.2 |
| 3 | Backend API — Spring Boot | `backend` | `apps/claims-api/` | Skeleton shipped, logic TBD | v0.3 → v0.5 |
| 4 | Frontend — React + TS | `frontend` | `apps/claims-web/` | Skeleton shipped, UI TBD | v0.3 → v0.5 |
| 5 | Data — schemas, migrations, masking | `data` | `data/`, `services/db-sync/` | Skeleton shipped, content TBD | v0.3 → v0.4 |
| 6 | GenAI gateway + RAG | `platform-genai` | `services/genai-gateway/` | Skeleton shipped, gateway code TBD | v0.4 |
| 7 | CI/CD — Jenkins pipeline + shared library | `platform-cicd` | `ci/` | Skeleton shipped, pipeline logic TBD | v0.3 |
| 8 | SRE / Observability | `platform-sre` | `infra/monitoring/`, `infra/systemd/`, `infra/timezone/` | Skeleton shipped, content TBD | v0.3 |
| 9 | Security baseline | `platform-sec` | `infra/firewall/`, `services/auth-realm/`, `security-baseline.md` | Skeleton shipped, rules TBD | v0.3 |
| 10 | Test infrastructure | `qa` | `tests/`, CI integration | Skeleton shipped, suites TBD | v0.4 → v0.5 |
| 11 | Operations & runbooks | `sre` | `docs/runbooks/`, `ops/` | Not started | v0.5 |
| 12 | Final review (pack acceptance) | `verifier` | All | Not started | v0.6 → v1.0 |

## Dependency graph (build order)

```
[1. Architecture]  ────────────────────────────────────────────────► (ongoing)
        │
        ▼
[2. Domain model]  ──────────────┐
        │                        │
        ▼                        ▼
[3. Backend API]  ◀───────  [5. Data]
        │                        │
        ▼                        │
[10. Test infra]  ───────────────┤
        │                        │
        ▼                        ▼
[6. GenAI gateway]  ◀───────  [9. Security baseline]
        │                        │
        ▼                        │
[4. Frontend]  ◀─────────────────┘
        │
        ▼
[7. CI/CD pipeline]   (integrates all of the above)
        │
        ▼
[8. Observability]    (depends on services existing)
        │
        ▼
[11. Runbooks]        (depends on prod-shaped system)
        │
        ▼
[12. Final review]
```

Critical path: 1 → 2 → 3 → 10 → 4 → 7 → 8 → 12.

## Per-workstream acceptance criteria

### 1. Architecture & ADR ledger

- Every confirmed requirement in `01_ARS.docx` is traceable to an
  ADR or a component.
- Every ADR has a status, a date, a context section, a decision,
  and consequences.
- `docs/architecture/adr/INDEX.md` is up to date.
- The pack's open decisions are listed in `INDEX.md` under
  "Deferred" with their next-ADR placeholder.

### 2. Domain model

- `docs/domain/claims-overview.md` describes the end-to-end flow
  in plain language with a Mermaid diagram.
- `docs/domain/entities.md` lists every entity, its attributes,
  its invariants, and the table it maps to.
- `docs/domain/workflows.md` describes each state machine with
  states, transitions, approvers, and GenAI touchpoints.
- `docs/domain/glossary.md` defines every domain term.

### 3. Backend API (Spring Boot)

- `mvn verify` is green on the skeleton.
- Package layout: `domain/`, `application/`, `infrastructure/`,
  `api/`, `config/`, `observability/`.
- `OpenAPI` spec is generated and checked in.
- Test pyramid in place: unit, integration (Testcontainers),
  contract (Pact provider).
- Endpoints: at least one per workflow in
  `docs/domain/workflows.md`.
- Coverage ≥ 80% line / 70% branch.

### 4. Frontend (React + TS)

- `pnpm build` and `pnpm typecheck` are green.
- Routes match the API surface.
- Component library: built on Radix UI primitives + Tailwind
  (or plain CSS modules — TBD, not in scope for foundation).
- E2E tests in `tests/e2e/` cover the critical user journeys.
- Coverage ≥ 70% line.

### 5. Data — schemas, migrations, masking

- Flyway migrations compile and run on a fresh Postgres 16.
- Schema is identical between qa and prod (data differs).
- `data/masking/rules.yaml` covers every PII column with a
  declared strategy.
- The `db-sync` Jenkins job has a working pipeline definition.
- Integrity check is implemented and unit-tested.

### 6. GenAI gateway + RAG

- LiteLLM is configured with at least one provider backend.
- Provider swap is a config-only operation.
- The data-class matrix from `ADR-011` is enforced by a
  pre-request policy check.
- The audit log records every prompt, response, and provider
  metadata.
- RAG over pgvector works against the QA DB (for development)
  and the prod DB (read-only, masked view).

### 7. CI/CD pipeline

- Jenkins is up and configured via JCasC.
- The Shared Library exposes `promoteToQA`, `promoteToUAT`,
  `promoteToProd`, `rollbackFrom<Env>`.
- A real PR triggers a real pipeline run that ends in a green
  build.
- Approvals are Keycloak-gated.

### 8. SRE / Observability

- Prometheus scrapes every host.
- Loki ingests logs from every host.
- Alertmanager routes by severity and rotation.
- The default alert set in `observability-design.md` is
  implemented.
- Grafana dashboards are provisioned from JSON in
  `infra/monitoring/grafana/dashboards/`.

### 9. Security baseline

- `infra/firewall/rules.template` is the single source of truth
  for nftables rules.
- Keycloak realm is exported to `services/auth-realm/`.
- All non-public endpoints are OIDC-protected.
- Egress from every host is default-deny except the GenAI
  gateway.

### 10. Test infrastructure

- Testcontainers is set up for Postgres, Keycloak, MinIO.
- Pact provider is wired into the CI pipeline.
- Playwright is wired into the CI pipeline.
- k6 is installed and one baseline scenario exists.

### 11. Operations & runbooks

- Every alert has a runbook link.
- Every failure mode listed in `11_Operations_Runbooks.docx` §2
  has a runbook.
- Oncall rotation is documented.
- Incident response templates are in `ops/incident-response/`.

### 12. Final review

- All pack acceptance criteria (`01_ARS.docx` §6) are met.
- All ADRs are Accepted or Superseded (no open Proposals).
- All workstreams 1–11 are Done.
- A walk-through of the system on a fresh dev environment
  matches the architecture.

## Hand-off contract (template)

When a workstream hands off to another:

1. **PR is merged** to `main`.
2. **CHANGELOG.md** is updated.
3. **The downstream workstream is named** in the PR description.
4. **A demo / evidence** is attached (build log, screenshot, curl
   trace, test run).
5. **Any new ADR** is opened in the same PR or referenced by
   number.

## Cadence

- **Daily standup (async):** each active workstream posts a 3-line
  status to the team channel: yesterday / today / blockers.
- **Weekly architecture review:** the architect agent posts the
  ADR index diff for the week and any open proposals.
- **End of each workstream:** the workstream owner writes a
  hand-off note into `docs/architecture/handoffs/` (TBD).

## When a workstream is blocked

- Open a `?` issue with the **workstream-blocked** label.
- If the block lasts more than 2 cycles, escalate to the architect
  agent: is the scope right, is the hand-off contract right, is
  the dependency graph right.
- Never silently grow scope. The hand-off contract is a contract.

## Status legend

- **Not started** — workstream has not begun.
- **Skeleton shipped** — directory + scaffolding exists; business
  value not yet delivered.
- **In progress** — actively being built.
- **Done (foundation)** — the foundation pass is complete; ongoing
  maintenance.
- **Done** — acceptance criteria met, hand-off delivered.
- **Blocked** — explicit blocker; see workstream channel.
