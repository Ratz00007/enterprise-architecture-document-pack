# ADR-007: Two database instances

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-007**, with
> instance-topology and engine choice captured under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-007
- **Supersedes:** —

## Context

The pack confirms two database instances only: **QA** and
**Production**. There is no Dev database and no UAT-specific
database (UAT uses a sanitized image of Production, see `ADR-008`).
This is a confirmed scope decision, not an oversight.

## Decision

- Exactly two logical database instances exist: `qa-db` and
  `prod-db`.
- `dev` uses ephemeral local databases (Testcontainers, in-process
  H2 for unit tests) — there is no shared dev database. Dev data
  is throwaway.
- `uat` runs against its own instance, but that instance is
  hydrated from a sanitized prod image, not maintained
  independently. From a logical-instance count, UAT reuses the
  "QA family" of databases (one physical instance per environment
  family, two families: QA-family and Prod-family).
- Engines: **PostgreSQL 16** on both. Same major version, same
  extensions, same schema. The schemas are managed by Flyway and
  are environment-agnostic.

## Consequences

**Positive**

- Two instances means two backup stories, two monitoring stories,
  two failover (or not) stories. Easy to reason about.
- No "which DB is the truth" ambiguity. Prod is the truth; QA is a
  derived state.

**Negative**

- No dev database means no cross-developer shared state. This is a
  *feature* in disguise — it forces dev to use migrations and
  fixtures, not "the DB already has the row".
- The QA-family (qa + uat) sharing a single instance approach is a
  possible future refinement; for the MVP we keep them on
  separate physical hosts because the pack says "one server per
  environment" and we follow that.

**Neutral**

- "Two database instances" is interpreted as "two logical
  instances". The UAT database is logically a separate instance
  (separate host, separate data) even though its data is
  derived from prod.

## Operationalisation

- Engine: **PostgreSQL 16** with `pgvector` extension enabled on
  both instances.
- Migrations: **Flyway 10**, source of truth in
  `data/schemas/{qa,prod}/`. Schema files are identical between
  the two environments; only the data differs.
- Backups: `prod-db-01` archives WAL continuously to
  `prod-db-bkp-01` via `pgBackRest`. `qa-db-01` has a daily logical
  dump, no WAL archive.
- Monitoring: `postgres_exporter` on both instances, scraped by
  `mon-01`.

## References

- `01_ARS.docx` §13
- `02_HLA.docx` §3
- `06_Data_Architecture.docx` §1
- `10_RTM_ADR.docx` §2 ADR-007
- `ADR-008` (prod image to QA)
- `network-topology.md`
