# Privacy — GDPR / CCPA Mapping

> Draft. To be reviewed by the data protection officer (DPO) and
> the legal team before the first data subject request.

## Data inventory

See `docs/domain/entities.md` for the schema. The fields on the
PII deny-list:

| Field | Class | Storage | Default access | Erasure strategy |
|-------|-------|---------|----------------|------------------|
| `parties.tax_id_hash` | high | Postgres, encrypted at rest | claims roles only | hash deletion is irreversible; we delete the row + tombstone |
| `parties.email` | high | Postgres, encrypted at rest | claims roles only | overwrite with `[REDACTED]@redacted` |
| `parties.phone` | high | Postgres, encrypted at rest | claims roles only | overwrite with `[REDACTED]` |
| `addresses.line1/line2/postal_code` | high | Postgres, encrypted at rest | claims roles only | delete the address row |
| `documents.storage_uri` | object storage | MinIO/S3 | claims roles only | delete the object + the row |
| `claims.description` | medium | Postgres | claims roles only | overwrite with `[REDACTED]` |
| `audit_log.payload` | varies | Postgres, append-only | auditor only | retain (compliance); PII within payload is redacted on export |

## Subject rights

| Right | Implementation | TTR |
|-------|----------------|-----|
| Right of access (Art. 15) | Self-service portal (TBD) + DPO email | 30 days |
| Right to erasure (Art. 17) | Soft-delete in prod (tombstone), hard-delete in QA/UAT runs of the db-sync job | 30 days |
| Right to rectification (Art. 16) | Self-service portal | 30 days |
| Right to portability (Art. 20) | Export endpoint (TBD) | 30 days |
| Right to object (Art. 21) | DPO email | 30 days |

## Cross-border transfers

None. The system is on-prem only, by confirmed design (`ADR-001`).

## Logging and auditing

Every state-mutating action writes a row to `audit_log` (see
`services/audit-log/`). The audit log is append-only, hash-chained,
and retained for 7 years.

## Breach notification

- Internal escalation: security lead (Sev-1 page) → CISO → CEO.
- External notification: within 72 hours of awareness, per GDPR.
- See `ops/incident-response/comms.md` for the template.
