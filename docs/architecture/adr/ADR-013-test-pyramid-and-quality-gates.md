# ADR-013: Test pyramid and quality gates

- **Status:** Accepted
- **Date:** 2026-08-25
- **Deciders:** Mavis (architect lead)
- **Supersedes:** —
- **Superseded by:** —

## Context

The pack requires both automated and manual testing
(`07_Test_Strategy.docx` §1). It does not yet specify coverage
thresholds, the test framework, the test data strategy, or the
exact quality gates per environment. This ADR fills those gaps in a
way that is consistent with `ADR-010` (no invented NFRs) and
`ADR-002` (sequential environments with evidence at each gate).

## Decision

### Test pyramid

```
                ┌──────────────┐
                │  E2E (web)   │  Playwright + Cucumber
                │   slow, few  │
                ├──────────────┤
                │  Contract    │  Pact (provider-driven)
                │  per service │
                ├──────────────┤
                │ Integration  │  Testcontainers
                │  (API+DB)    │
                ├──────────────┤
                │   Unit       │  JUnit 5 / Vitest
                │  fast, many  │
                └──────────────┘
```

- **Unit:** pure logic, no I/O. The majority of the test code.
- **Integration:** API + Postgres + Keycloak + MinIO, all in
  Testcontainers. One container set per test class.
- **Contract:** the API declares a Pact contract; consumers verify
  against the provider's published Pact. Provider-side runs in CI
  on every build.
- **E2E:** Playwright + Cucumber. Critical user journeys only
  (FNOL happy path, adjuster adjudication, payout). Runs against
  UAT, blocks UAT → Prod promotion.

### Coverage gates (line / branch)

| Component | Line | Branch | Notes |
|-----------|------|--------|-------|
| `apps/claims-api` | 80% | 70% | Build fails below. |
| `apps/claims-web` | 70% | n/a | Branch coverage tracked but not gated. |
| `services/genai-gateway` | 75% | 65% | Critical path. |
| `services/db-sync` | 80% | 70% | Data-integrity path. |

> These are *quality bars*, not invented NFRs. They are the
> minimum to keep the codebase reviewable. The pack does not say
> what coverage we need; we set it to a number that is
> industry-standard for a system of this kind.

### Per-environment quality gate

| Gate | Required evidence |
|------|-------------------|
| Dev → QA | Build green, unit + integration green, contract green (provider side), coverage gates met, SAST clean, dep-CVE scan clean, image signed |
| QA → UAT | All of the above + manual QA pass recorded in the QA portal, no `WARN` alerts in the smoke window |
| UAT → Prod | All of the above + UAT sign-off from at least one named UAT user per workflow, no `ERROR` alerts in the last 24h, rollback rehearsal in the last 30 days, release runbook reviewed |

### Test data

- **Unit tests** use fixtures defined inline (no shared state).
- **Integration tests** use Testcontainers + Flyway-managed
  Postgres. Each test class starts from a known migration state.
- **E2E tests** use a dedicated UAT user cohort. They never touch
  real PII; the QA / UAT DB is sanitized (`ADR-008`).
- **Load tests** are run against UAT only, on demand, with k6
  scripts in `tests/load/`.

## Consequences

**Positive**

- The test pyramid is industry-standard. New engineers ramp
  quickly.
- The gates are tied to the promotion pipeline, so a "test
  passed" claim is backed by a CI run, not a human assertion.

**Negative**

- The coverage thresholds will be controversial. We will be asked
  "why 80 and not 90". The answer is: this is the floor, not the
  target. The codebase should trend up, not oscillate around the
  floor.

**Neutral**

- Manual QA is a first-class stage. It is not a backstop for
  missing automation; it is the verification step that no
  automation can replace (the pack is explicit on this).

## Operationalisation

- Test commands in `AGENTS.md`.
- Coverage tool: `jacoco` for the API, `vitest --coverage` for
  the web.
- Gate enforcement: in `ci/Jenkinsfile`, every promotion step
  runs the gate and fails the build on miss.
- Test data: `data/seed/` and `tests/fixtures/`.

## References

- `01_ARS.docx` §12
- `04_CICD_DevOps.docx` §2, §4
- `07_Test_Strategy.docx` (entire document)
- `10_RTM_ADR.docx` §2
- `ADR-002` (sequential environments)
- `ADR-006` (human approval)
- `tests/`
