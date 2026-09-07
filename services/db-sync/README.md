# services/db-sync — Production → QA image refresh (ADR-008)

`sync-prod-to-qa.sh` implements the pipeline the pack requires
(`06_Data_Architecture.docx` §4): snapshot the approved production image,
restore it into a staging schema on the QA instance, mask PII, validate,
check integrity, and record provenance.

## Flow

1. **Snapshot** — `pg_dump` of the production database (read-only; the script
   never writes to production).
2. **Restore** — schema + data into `sync_staging` on the QA instance.
3. **Mask** — generates UPDATE statements from
   `data/masking/masking-rules.json`: `hash_with_domain_preservation` becomes
   deterministic salted hashing, `date_offset` becomes hash-derived day
   offsets, and free-text claim columns get the universal PII sweep (SSN,
   card, email, phone patterns — same rules the API's `PIIMaskingService`
   enforces, ADR-017).
   Techniques that require an HSM (e.g. `FF3-1`) deliberately fall back to
   deterministic hashing in-pipeline; a "real" FFE needs the HSM key material
   and is a platform-team configuration, not code.
4. **Validate** — runs `data/scripts/validate-masking.sh` against the QA
   instance; any detected raw PII fails the run.
5. **Integrity** — row counts must match production per masked table.
6. **Provenance** — every run records source host, rules version and swap
   status into `sync_provenance`.

## The swap is a human decision

By default the masked copy stays in `sync_staging` for review. Only a
deliberate re-run with `--swap` (inside a maintenance window) moves it into
the `public` schema, keeping the previous tables as `*_pre_sync` for
rollback — the compensating control for the weak network isolation called
out in DINA §5.

```bash
# refresh + mask + validate; leave result in staging for review
./sync-prod-to-qa.sh --prod-host proddb1 --qa-host qadb1

# after review, in a maintenance window
./sync-prod-to-qa.sh --prod-host proddb1 --qa-host qadb1 --swap
```

Requires `pg_dump`, `psql`, `jq`; credentials via `.pgpass`/`PGPASSWORD`.

## Scheduling

The Jenkins QA job owns the cadence (proposal: weekly, before QA test
cycles); wiring it as a pipeline step is tracked in the README status table.
