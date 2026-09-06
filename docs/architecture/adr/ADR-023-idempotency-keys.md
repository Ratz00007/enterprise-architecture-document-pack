# ADR-023: Idempotency keys via an idempotency_keys table

- **Status:** Accepted
- **Date:** 2026-09-06
- **Origin:** This project (operationalises the AGENTS.md API rule)
- **Supersedes:** —

## Context

AGENTS.md requires idempotency keys on state-mutating endpoints. Claim
creation and lifecycle transitions must not execute twice when a client
retries (Jenkins promotion retries, flaky networks, adjuster double-clicks).

## Decision

- Every state-mutating endpoint requires an `Idempotency-Key` header.
- The first execution stores `(idempotency_key, endpoint, request_hash,
  response_status, response_body)` in the `idempotency_keys` table inside the
  same transaction as the mutation.
- A retry with the same key on the same endpoint and same request hash
  replays the stored response verbatim, with an
  `Idempotency-Replayed: true` header.
- The same key with a different request hash (or on a different endpoint) is a
  conflict: HTTP 409.

## Consequences

- Replay is exact, including the original status code.
- Duplicate protection spans service restarts and both app servers; no
  in-memory cache can be lost.
- Entries currently have no TTL; a retention rule can be added later without
  touching the API contract.
