# ADR-010: No invented NFRs (performance, HA, advanced security)

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-010 (`01_ARS.docx` §2: "No specific scalability, availability, performance or advanced security targets were supplied; do not invent them")
- **Supersedes:** —

## Context

The stakeholder explicitly supplied no scalability, availability, performance
or advanced-security targets. AGENTS.md forbids inventing scope or targets the
pack does not state.

## Decision

- No performance, HA, DR or advanced-security NFR is claimed, designed or
  tested beyond the pack baseline (firewall + IP/port allow-lists, monitoring).
- Where a target is genuinely needed (e.g. hardware sizing, RPO/RTO), the ADR
  is marked TBD and waits for stakeholder input (ADR-014, ADR-020).

## Consequences

- Agent and human contributors must not add benchmarks, SLOs or security
  controls "for completeness".
- Any NFR a workstream needs must enter through a new ADR, not a code comment.
