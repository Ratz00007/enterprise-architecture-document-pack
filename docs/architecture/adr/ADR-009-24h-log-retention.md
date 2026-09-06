# ADR-009: 24×7 logging with 24-hour retention/reset

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-009 (`01_ARS.docx` REQ-17: "24x7 continuous log generation; log-file retention/reset period is 24 hours")
- **Supersedes:** —

## Context

The stakeholder requires continuous log generation with a 24-hour
retention/reset period. This is unusual for an enterprise system but is the
stated requirement; longer retention was not requested and must not be
invented (ADR-010).

## Decision

- All environment servers generate logs 24×7.
- Raw log files rotate and are reset every 24 hours.
- The separate monitoring capability (REQ-18) works from this stream; alerts
  are decoupled from raw-log retention.

## Consequences

- Raw logs older than 24 hours are gone; anything requiring longer retention
  (audit, incident follow-up) must be exported deliberately and explicitly
  approved — that is new scope, not an implementation detail.
- Loki is configured with a 24h retention window per environment.
