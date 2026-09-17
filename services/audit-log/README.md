# Audit Log

Append-only, hash-chained audit trail. Every state-mutating action
in the system writes a row to the `audit_log` table in the same DB
transaction as the action itself.

## Properties

- **Write-once.** Database trigger rejects `UPDATE` and `DELETE`.
- **Tamper-evident.** Every row stores a `hash` of itself and a
  `prev_hash` pointing to the previous row. Any retroactive edit
  breaks the chain.
- **Long retention.** 7 years (compliance-aligned).
- **Read-only API** (post-MVP) for auditors, with paging + export.

## Writers

| Writer | Trigger |
|--------|---------|
| `apps/claims-api` | Every state-mutating endpoint, in the same transaction |
| `ci/jenkins/shared-library` | Every promotion approval, promotion, rollback |
| `services/genai-gateway` | Every prompt + every response |
| `services/db-sync` | Every sync run, with provenance |

## Schema

The authoritative schema is `apps/claims-api/src/main/resources/db/migration/V1__audit_log.sql`
(TBD in the data workstream). The shape:

```
audit_log(
  id              uuid PK,
  occurred_at     timestamptz not null,
  actor_subject   text not null,
  actor_role      text,
  action          text not null,
  resource_type   text not null,
  resource_id     uuid not null,
  correlation_id  uuid,
  payload         jsonb,
  prev_hash       text,
  hash            text not null
)
```

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Schema | Open | Migration in data workstream |
| API write path | Drafted | See `apps/claims-api/.../audit/AuditWriter.java` (TBD) |
| API read path | Open | Auditor-only |
| Archive | Open | Ship to WORM block storage at 1 year |

## References

- `ADR-006` Human approval
- `ADR-011` GenAI data boundary
- `docs/domain/entities.md` (`audit_log` definition)
- `docs/compliance/soc2.md` (TBD)
