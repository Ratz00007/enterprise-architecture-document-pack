# ADR-012: Application domain — insurance claims processing (FNOL → adjudication → payout)

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** This project (locked in by stakeholder)
- **Supersedes:** —

## Context

The architecture pack describes the infrastructure for "one application" but
leaves the domain open. The stakeholder locked the domain to insurance claims
processing.

## Decision

- The application domain is **insurance claims processing** covering the
  lifecycle FNOL → triage → adjudication → payout (README, AGENTS.md).
- The executable domain model and its lifecycle are defined in code with a
  recorded state machine; lifecycle specifics beyond the pack wording are
  captured in ADR-021.
- Scope, vendors and performance targets beyond this domain are not invented
  (ADR-010).

## Consequences

- Domain workstreams (API, web, testing) derive requirements from this ADR and
  the pack; anything else is new scope and needs stakeholder approval.
