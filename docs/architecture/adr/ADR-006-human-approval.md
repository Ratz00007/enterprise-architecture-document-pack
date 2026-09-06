# ADR-006: Human approval mandatory for every promotion

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-006 (`01_ARS.docx` REQ-11: "Human approval must remain in the promotion/release process")
- **Supersedes:** —

## Context

The pack requires human approval in the promotion/release process. Environments
progress sequentially (Dev → QA → UAT → Production), and no stakeholder asked
for autonomous deployment.

## Decision

- Every promotion step in the pipeline stops at a manual approval gate.
- No environment promotes to the next one without a human sign-off, recorded
  in the pipeline history.
- GenAI may assist analysis, but never approves a promotion (see ADR-006
  and ADR-011: GenAI is advisory only).

## Consequences

- The Jenkins pipeline implements `input` gates before QA, UAT and Prod.
- Deployment lead time is bounded below by human availability; this is
  accepted and is not an NFR violation (ADR-010).
- Approval identity and timestamp are auditable from the CI system.
