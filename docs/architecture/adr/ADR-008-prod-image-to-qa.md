# ADR-008: Production image to QA (controlled)

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-008**, with the
> concrete sanitization + pipeline mechanism captured under
> "Operationalisation".

- **Status:** Accepted (mechanism TBD)
- **Date:** 2026-08-25
- **Origin:** Pack ADR-008
- **Supersedes:** —

## Context

The pack confirms a one-directional data flow: a sanitized image of
the production database is made available to the QA environment
(including UAT, see `ADR-007`). The flow goes through the pipeline
— not ad-hoc — and is the basis for QA testing on real-world data
shapes.

The critical open question (`06_Data_Architecture.docx` §3) is
whether the QA image is a literal copy or a masked / sanitized
derivative. The architecture treats the latter as the safe default
and captures the choice here.

## Decision

- The flow is one-directional: **Prod → QA-family** (qa + uat).
- The image is **always sanitized**, never a literal copy. PII
  fields are masked, identifiers are pseudonymized, free-text
  fields are either dropped or redacted.
- The pipeline is the `db-sync` Jenkins job
  (`services/db-sync/`), running on a schedule (default: weekly,
  Sunday 02:00 UTC) and on-demand (post-incident).
- The job authenticates to `prod-db-01` as a read-only role,
  exports a logical dump, runs the masking pipeline in an
  isolated workspace, then restores into `qa-db-01` (and
  `uat-db-01` if the run is for the UAT family).
- Every run writes a provenance record to the audit log: source
  prod backup ID, masking rules applied (by version), destination
  environment, run duration, row counts before/after, integrity
  check results.
- Integrity check = row-count delta vs. last successful run,
  foreign-key check, sample-aggregate sanity check. Any failure
  blocks the restore.

## Consequences

**Positive**

- QA and UAT see a realistic data shape without ever touching
  real PII.
- The pipeline is auditable end to end. Every byte that landed
  in QA can be traced to a specific prod backup.
- The sanitization rules are versioned in git, so changes are
  reviewed and reproducible.

**Negative**

- Sanitization is *work*. Every new PII field needs a masking
  rule. We will miss some; the audit log + the QA team's manual
  review are the safety net.
- Performance: a full sanitized restore is not free. The job is
  scheduled out of hours to avoid contention.
- Schema drift between prod and the masking ruleset is a real
  failure mode. The integrity check catches the worst cases, but
  not all.

**Neutral**

- The mechanism is a Jenkins job for MVP. Post-MVP we may move to
  a dedicated service; that would be a new ADR, not an edit of
  this one.

## Operationalisation

- Job: `db-sync` in the Jenkins Shared Library.
- Source role: `readonly_replicator` on `prod-db-01`, time-bounded
  credentials issued by Keycloak at job start.
- Masking rules: `data/masking/rules.yaml` (declarative list of
  table.column → strategy: `drop | hash | redact | tokenize |
  keep`).
- Restoration: `pg_restore` into a freshly-created database, then
  `ANALYZE`, then the integrity check.
- Evidence: every run produces a JSON report attached to the
  audit log entry.

### Open items for the next revision

- Exact masking strategy per PII field (TBD with data
  classification). Captured as ADR-017 (TBD).
- Whether the prod-image-to-UAT family uses the same masking
  ruleset or a stricter one. (TBD with the UAT lead; default =
  same ruleset, with an additional row-count limit.)

## References

- `01_ARS.docx` §14
- `06_Data_Architecture.docx` (entire document)
- `10_RTM_ADR.docx` §2 ADR-008
- `ADR-007` (two DB instances)
- `data/masking/`
- `services/db-sync/`
