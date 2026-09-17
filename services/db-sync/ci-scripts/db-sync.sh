#!/usr/bin/env bash
# db-sync.sh — sanitize prod -> qa/uat.
# Run by Jenkins on a schedule or on demand. Never run by hand
# without telling the team.

set -euo pipefail

ENV_TARGET="${1:-qa}"   # qa | uat
MASKING_RULES_VERSION="${2:-HEAD}"
JOB_START_TS=$(date -u +%FT%TZ)

echo "[db-sync] env=${ENV_TARGET} rules_version=${MASKING_RULES_VERSION} start=${JOB_START_TS}"

# 1. Read-only credentials for prod (time-bounded via Keycloak).
PROD_CONN=$(issue_time_bounded_credentials prod-db-01 readonly_replicator 30m)
# 2. Write credentials for the target.
TARGET_CONN=$(issue_time_bounded_credentials "${ENV_TARGET}-db-01" sync_writer 30m)

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# 3. Dump.
pg_dump --no-owner --no-privileges --format=custom \
    --file="$WORK/dump.pgdump" "$PROD_CONN"

# 4. Mask.
psql "$PROD_CONN" -At -c "SELECT count(*) FROM claims" > "$WORK/source_row_count"
psql "$PROD_CONN" -At -c "SELECT count(*) FROM policies" >> "$WORK/source_row_count"

apply_masking "$WORK/dump.pgdump" "$WORK/masked.pgdump" "$MASKING_RULES_VERSION"

# 5. Restore.
pg_restore --no-owner --no-privileges --clean --if-exists \
    --dbname="$TARGET_CONN" "$WORK/masked.pgdump"

# 6. Integrity check.
integrity_check "$TARGET_CONN" "$WORK/source_row_count"

# 7. Audit.
write_audit_log \
    action=db_sync \
    env="$ENV_TARGET" \
    rules_version="$MASKING_RULES_VERSION" \
    source="prod-db-01" \
    target="${ENV_TARGET}-db-01" \
    job_start_ts="$JOB_START_TS" \
    job_end_ts=$(date -u +%FT%TZ) \
    row_counts="$(cat "$WORK/source_row_count")" \
    integrity_check=ok

echo "[db-sync] done"
