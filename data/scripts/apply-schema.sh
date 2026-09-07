#!/bin/bash
# =============================================================================
# Apply the operational schema (data/schemas/*.sql) to a database in filename
# order. Idempotent per-file via IF NOT EXISTS semantics; 001 uses CREATE
# TABLE without IF NOT EXISTS, so files already applied are recorded here.
# =============================================================================
set -euo pipefail

SCHEMA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../schemas" && pwd)"
DB_HOST="${1:-localhost}"
DB_PORT="${2:-5432}"
DB_NAME="${3:-acme_claims}"
DB_USER="${4:-acme_user}"

command -v psql >/dev/null || { echo "psql is required"; exit 2; }

psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE IF NOT EXISTS schema_history (
  filename TEXT PRIMARY KEY,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
SQL

for file in "${SCHEMA_DIR}"/*.sql; do
  filename="$(basename "${file}")"
  APPLIED=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -t -A -c \
    "SELECT count(*) FROM schema_history WHERE filename = '${filename}'")
  if [[ "${APPLIED}" != "0" ]]; then
    echo "Skipping ${filename} (already applied)"
    continue
  fi
  echo "Applying ${filename}"
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${file}"
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -c \
    "INSERT INTO schema_history (filename) VALUES ('${filename}')"
done

echo "Schema up to date."
