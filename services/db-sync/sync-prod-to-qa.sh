#!/bin/bash
# =============================================================================
# Production -> QA database image refresh (ADR-008, 06_Data_Architecture.docx)
# =============================================================================
# Flow: approved production dump -> restore into a staging schema on the QA
# instance -> mask PII (data/masking/masking-rules.json techniques) -> run
# validate-masking.sh -> integrity checks -> record provenance.
#
# The final swap into the QA public schema is EXPLICIT: run with --swap inside
# a maintenance window after a human has reviewed the staging copy. The script
# never writes to production.
#
# Usage:
#   ./sync-prod-to-qa.sh --prod-host P --qa-host Q [--swap]
# Requires: pg_dump, psql, jq; credentials via .pgpass or PGPASSWORD env.
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MASKING_RULES="${REPO_ROOT}/data/masking/masking-rules.json"
STAGING_SCHEMA="sync_staging"
QA_DB="${QA_DB:-acme_claims}"
PROD_DB="${PROD_DB:-acme_claims}"
PROD_HOST=""
QA_HOST=""
DO_SWAP=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --prod-host) PROD_HOST="$2"; shift 2 ;;
    --qa-host)   QA_HOST="$2"; shift 2 ;;
    --swap)      DO_SWAP=true; shift ;;
    *) echo "Unknown argument: $1"; exit 2 ;;
  esac
done

[[ -n "${PROD_HOST}" && -n "${QA_HOST}" ]] || { echo "--prod-host and --qa-host are required"; exit 2; }
command -v jq >/dev/null || { echo "jq is required"; exit 2; }

DUMP_FILE="$(mktemp -t acme-prod-dump-XXXXXX.sql)"
trap 'rm -f "${DUMP_FILE}"' EXIT

echo "[1/6] Snapshotting approved production image from ${PROD_HOST}/${PROD_DB}"
pg_dump -h "${PROD_HOST}" -U "${PROD_DB_USER:-acme_user}" -d "${PROD_DB}" \
  --no-owner --no-privileges --clean --if-exists -f "${DUMP_FILE}"

echo "[2/6] Restoring into staging schema ${STAGING_SCHEMA} on ${QA_HOST}"
psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -v ON_ERROR_STOP=1 <<SQL
DROP SCHEMA IF EXISTS ${STAGING_SCHEMA} CASCADE;
CREATE SCHEMA ${STAGING_SCHEMA};
SQL
pg_dump -h "${PROD_HOST}" -U "${PROD_DB_USER:-acme_user}" -d "${PROD_DB}" --schema-only --no-owner -f - \
  | sed "s/public/${STAGING_SCHEMA}/g" \
  | psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -v ON_ERROR_STOP=1

echo "[3/6] Applying masking rules from ${MASKING_RULES}"
MASK_SQL="$(mktemp -t acme-mask-XXXXXX.sql)"
trap 'rm -f "${DUMP_FILE}" "${MASK_SQL}"' EXIT

# Technique -> SQL expression map. Techniques that need HSM keys (FF3-1) are
# intentionally NOT implemented in-pipeline: their columns fall back to
# deterministic hashing, and the rules file documents the real technique.
jq -r '
  .entities | to_entries[] | .key as $table | .value.fields | to_entries[] |
  select(.value.classification == "PII") |
  "\($table)|\(.key)|\(.value.technique)"
' "${MASKING_RULES}" | while IFS='|' read -r table column technique; do
  # Only emit SQL for tables/columns that exist in the restored schema.
  EXISTS=$(psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -t -A -c \
    "SELECT count(*) FROM information_schema.columns
     WHERE table_schema='${STAGING_SCHEMA}' AND table_name='${table}' AND column_name='${column}'")
  [[ "${EXISTS}" == "1" ]] || continue
  case "${technique}" in
    hash_with_domain_preservation)
      echo "UPDATE ${STAGING_SCHEMA}.${table} SET ${column} = 'masked_' || substr(md5(${column}::text || id::text), 1, 12) WHERE ${column} IS NOT NULL;" >> "${MASK_SQL}" ;;
    date_offset)
      echo "UPDATE ${STAGING_SCHEMA}.${table} SET ${column} = ${column} + (interval '1 day' * ((hashnumextended(${column}::text::bytea, 240) % 241) - 120));" >> "${MASK_SQL}" ;;
    *)
      echo "UPDATE ${STAGING_SCHEMA}.${table} SET ${column} = md5(${column}::text || id::text) WHERE ${column} IS NOT NULL;" >> "${MASK_SQL}" ;;
  esac
done

# Universal PII sweep on free-text columns that carry claim content.
for TEXTCOL in description adjudication_notes rejection_reason; do
  EXISTS=$(psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -t -A -c \
    "SELECT count(*) FROM information_schema.columns
     WHERE table_schema='${STAGING_SCHEMA}' AND table_name='claims' AND column_name='${TEXTCOL}'")
  [[ "${EXISTS}" == "1" ]] || continue
  cat >> "${MASK_SQL}" <<SQL
UPDATE ${STAGING_SCHEMA}.claims SET ${TEXTCOL} =
  regexp_replace(regexp_replace(regexp_replace(regexp_replace(${TEXTCOL},
    '\m\d{3}-\d{2}-\d{4}\M', '***-**-****', 'g'),
    '\m(?:\d{4}[- ]?){3}\d{4}\M', '****-****-****-****', 'g'),
    '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}', '***@***.***', 'g'),
    '\m\d{3}[-.]?\d{3}[-.]?\d{4}\M', '***-***-****', 'g')
  WHERE ${TEXTCOL} IS NOT NULL;
SQL
done

psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -v ON_ERROR_STOP=1 -f "${MASK_SQL}"

echo "[4/6] Validating masking (data/scripts/validate-masking.sh)"
"${REPO_ROOT}/data/scripts/validate-masking.sh" qa "${QA_HOST}"

echo "[5/6] Integrity checks: row counts production vs masked staging"
jq -r '.entities | keys[]' "${MASKING_RULES}" | while read -r table; do
  IN_STAGING=$(psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -t -A -c \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema='${STAGING_SCHEMA}' AND table_name='${table}'")
  [[ "${IN_STAGING}" == "1" ]] || continue
  PROD_ROWS=$(psql -h "${PROD_HOST}" -U "${PROD_DB_USER:-acme_user}" -d "${PROD_DB}" -t -A -c "SELECT count(*) FROM ${table}")
  STAGE_ROWS=$(psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -t -A -c "SELECT count(*) FROM ${STAGING_SCHEMA}.${table}")
  if [[ "${PROD_ROWS}" != "${STAGE_ROWS}" ]]; then
    echo "INTEGRITY FAILURE: ${table} prod=${PROD_ROWS} staging=${STAGE_ROWS}"; exit 1
  fi
  echo "  ${table}: ${STAGE_ROWS} rows OK"
done

echo "[6/6] Recording provenance"
psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -v ON_ERROR_STOP=1 <<SQL
CREATE TABLE IF NOT EXISTS sync_provenance (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  source_host TEXT NOT NULL,
  staging_schema TEXT NOT NULL,
  masking_rules_version TEXT NOT NULL,
  swapped BOOLEAN NOT NULL DEFAULT false
);
INSERT INTO sync_provenance (source_host, staging_schema, masking_rules_version, swapped)
VALUES ('${PROD_HOST}', '${STAGING_SCHEMA}',
        '$(jq -r .version "${MASKING_RULES}")', ${DO_SWAP});
SQL

if [[ "${DO_SWAP}" == "true" ]]; then
  echo "Swapping ${STAGING_SCHEMA} into public (maintenance window operation)"
  TABLES=$(jq -r '.entities | keys[]' "${MASKING_RULES}")
  SWAP_SQL=""
  for table in ${TABLES}; do
    IN_STAGING=$(psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -t -A -c \
      "SELECT count(*) FROM information_schema.tables WHERE table_schema='${STAGING_SCHEMA}' AND table_name='${table}'")
    [[ "${IN_STAGING}" == "1" ]] || continue
    SWAP_SQL="${SWAP_SQL}
ALTER TABLE public.${table} RENAME TO ${table}_pre_sync;
ALTER TABLE ${STAGING_SCHEMA}.${table} SET SCHEMA public;"
  done
  psql -h "${QA_HOST}" -U "${QA_DB_USER:-acme_user}" -d "${QA_DB}" -v ON_ERROR_STOP=1 <<SQL
BEGIN;
${SWAP_SQL}
COMMIT;
SQL
  echo "Swap complete. Previous QA tables kept as *_pre_sync for rollback."
else
  echo "Staging copy ready in schema '${STAGING_SCHEMA}'. Review it, then re-run with --swap in a maintenance window."
fi

echo "Sync complete."
