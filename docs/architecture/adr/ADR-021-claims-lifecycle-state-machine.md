# ADR-021: Claim lifecycle state machine

- **Status:** Accepted
- **Date:** 2026-09-06
- **Origin:** This project (implementation of ADR-012)
- **Supersedes:** —

## Context

ADR-012 locks the domain to FNOL → triage → adjudication → payout, but the
pack does not enumerate the concrete states, whether a claim can be rejected,
or how a claim ends. The executable API needs exactly one answer.

## Decision

- States: `FNOL`, `TRIAGE`, `ADJUDICATION`, `APPROVED`, `REJECTED`, `PAYOUT`,
  `PAID`, `CLOSED`.
- Allowed transitions (enforced in `ClaimStatus`, applied only through
  `Claim.transitionTo`):
  - FNOL → TRIAGE
  - TRIAGE → ADJUDICATION
  - ADJUDICATION → APPROVED | REJECTED
  - APPROVED → PAYOUT
  - PAYOUT → PAID
  - PAID → CLOSED
  - REJECTED → CLOSED
  - CLOSED is terminal.
- Illegal transitions fail with HTTP 409; claims cannot be deleted — closure
  is the only end state (audit trail).

## Consequences

- Rejection and closure are the two additions to the pack wording; both are
  inherent to adjudication and end-of-life handling and are the minimal set
  that keeps the API honest. Any further states (e.g. reopen, appeal,
  litigation) are new scope requiring an ADR.
