# ADR-022: Money as BIGINT minor units

- **Status:** Accepted
- **Date:** 2026-09-06
- **Origin:** This project (operationalises the AGENTS.md data rule)
- **Supersedes:** —

## Context

Claim amounts (estimated, approved, paid) must be stored exactly. Floats and
`NUMERIC`/`DECIMAL` invite rounding drift across services and the analytics
copy (ADR-008); AGENTS.md already fixes the rule: money is BIGINT minor units.

## Decision

- All monetary values are stored as `BIGINT` **minor units** (cents for
  USD-class currencies) in columns `estimated_amount`, `approved_amount`,
  `paid_amount`.
- Java side uses `Long`; JSON fields carry the `Minor`/`minorUnits` suffix so
  no client can mistake them for major units.
- The platform is single-currency for v1; the currency is not stored per row.
  Multi-currency support is new scope and needs an ADR of its own.

## Consequences

- Presentation layers convert to major units at the edge only.
- Summing across rows is exact; no rounding rules to document.
