# ADR-013: Test pyramid, coverage gates, and required promotion checks

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** This project (operationalises pack REQ-12: automated + manual testing)
- **Supersedes:** —

## Context

The pack requires both automated and manual testing but does not prescribe a
test toolchain. AGENTS.md fixes the concrete gates; this ADR records why.

## Decision

- Test pyramid: unit (JUnit 5 / Vitest) → integration (Testcontainers) →
  contract (Pact) → E2E (Playwright/Cucumber) → load (k6).
- Coverage gates: API 80% line / 70% branch; web 70%.
- Security scanning (Trivy, OWASP Dependency-Check) runs on every build.
- A build that fails any gate cannot be promoted (ADR-006 gates are
  downstream of these checks).

## Consequences

- `mvn verify` is the single local entry point for API quality gates.
- Contract and E2E suites activate as the corresponding consumers/producers
  land; their absence is tracked per workstream, not assumed.
