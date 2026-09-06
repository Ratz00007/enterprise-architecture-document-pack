# ADR-024: UUIDv7 primary keys, generated application-side

- **Status:** Accepted
- **Date:** 2026-09-06
- **Origin:** This project (operationalises the AGENTS.md data rule)
- **Supersedes:** —

## Context

AGENTS.md mandates UUIDv7 primary keys. UUIDv4 (random) keys fragment B-tree
indexes on insert-heavy tables like `claims` and `idempotency_keys`, and
sequence IDs complicate the Production → QA image flow (ADR-008) where key
ranges must not collide across refreshes.

## Decision

- Primary keys are **RFC 9562 UUIDv7**, generated in the application
  (`com.acme.claims.util.UuidV7`) at persist time.
- No database extension is required; the schema keeps
  `DEFAULT gen_random_uuid()` purely as a safety net for non-application
  writers.

## Consequences

- Keys are roughly time-ordered: indexes stay compact, and `claims.id`
  ordering approximates creation order.
- Clock skew affects only ordering, not uniqueness.
- The generator is unit-tested for version/variant bits and time ordering.
