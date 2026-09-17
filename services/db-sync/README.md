# DB Image Sync (Prod → QA/UAT)

Pipeline-mediated, sanitized copy of the production database into
the QA and UAT instances. Driven by a Jenkins job
(`ci/scripts/db-sync.sh`) on a schedule and on demand.

## What it does

1. Reads from `prod-db-01` as the time-bounded `readonly_replicator`
   role.
2. Dumps a logical backup (pg_dump, custom format).
3. Runs the masking pipeline (`data/masking/rules.yaml`) over the
   dump in an isolated workspace.
4. Restores the masked dump into `qa-db-01` and/or `uat-db-01`.
5. Runs integrity checks (row counts, FK checks, sample aggregates).
6. Records provenance in the audit log.

## What it does NOT do

- It never copies raw production data. The masking step is
  mandatory; no path skips it.
- It never writes to the production database. The credentials used
  for the prod side are read-only.
- It never runs without an audit trail.

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Jenkins job | Drafted | See `ci/scripts/db-sync.sh` |
| Masking rules | Drafted | See `data/masking/rules.yaml` |
| Integrity checks | Open | FK + row count + sample aggregates |
| Schedule | Open | Default: weekly, Sunday 02:00 UTC |
| Notification | Open | Slack + audit log on every run |

## References

- `ADR-007` Two DB instances
- `ADR-008` Production image to QA
- `06_Data_Architecture.docx` (the pack)
- `data/masking/rules.yaml`
