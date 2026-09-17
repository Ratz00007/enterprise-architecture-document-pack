# ADR-009: 24-hour log retention / reset

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-009**, with the
> boundary semantics captured under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-009
- **Supersedes:** —

## Context

The pack requires 24×7 continuous log generation and a 24-hour
retention/reset window. The two things are distinct: the *generation*
is continuous (no gaps), the *retention* is bounded at 24 hours of
hot storage.

## Decision

- Every host emits logs continuously, 24×7. There is no scheduled
  silence.
- Hot retention is **24 hours** for raw logs (Loki + local journal).
  The 24-hour boundary is rotated, not dropped, by an Ansible
  role: the previous day's logs are shipped to long-term archive
  storage (S3-compatible block, WORM, 30-day retention) before
  being deleted from hot.
- The rotation is a copy-truncate at the journald layer so the
  boundary cannot drop a line in flight.
- 24 hours is the *raw* retention. **Derived events** (alert
  history, aggregated metrics, audit-log rows) have their own,
  longer retention policies documented in the SRE runbook.
- The 24-hour boundary is *not* applied to:
  - The audit log (retention 7 years, WORM).
  - The application data (retention per business policy).
  - Backup manifests (retention per backup policy).

## Consequences

**Positive**

- Matches the stakeholder direction exactly.
- The boundary is a single, well-defined point. "What happened at
  02:00 yesterday?" is answerable for 24 hours, then requires the
  archive.
- The rotation is auditable — the rotation job writes a record
  to the audit log on success and on failure.

**Negative**

- 24h raw retention is short for some investigation patterns. The
  archive is the answer; the runbook covers the steps.
- The boundary must be time-aligned across hosts. We run NTP via
  chrony against a single internal source. Drift > 250 ms fires
  an alert.

**Neutral**

- The "24h" is configurable per environment, but for the MVP
  every environment uses the same value to keep the runbook
  simple.

## Operationalisation

- Application: logback → stdout → journald → Promtail → Loki.
- System: rsyslog / journald, depending on the host. Same
  rotation behaviour.
- Rotation: `infra/ansible/roles/common/tasks/logrotate.yml` runs
  hourly, copy-truncates the previous hour's journal, ships to
  the archive, and verifies the upload before deleting the local
  copy.
- Alert: `LogRotationStuck` (Sev-2) fires if the rotation has not
  completed by the 25-hour mark.

## References

- `01_ARS.docx` §17
- `08_Observability.docx` §2
- `10_RTM_ADR.docx` §2 ADR-009
- `observability-design.md`
- `infra/monitoring/prometheus/rules/`
