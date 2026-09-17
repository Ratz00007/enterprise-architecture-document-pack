# Auth Realm (Keycloak)

Versioned export of the Keycloak realm that backs the OIDC SSO for
all services and humans.

## What's in here

- `realm-export.json` — the realm definition (clients, roles,
  groups, identity providers, required actions, authentication
  flows, brute-force detection, password policy).
- `themes/` — any custom themes (we use the default Keycloak theme
  for MVP).
- `smtp/` — SMTP settings (post-MVP, not in the export).

## Roles

| Role | Grants |
|------|--------|
| `developer` | dev app read/write; QA read; no prod |
| `qa_lead`   | dev app read; QA write + approve; UAT read; no prod |
| `uat_user`  | UAT app read/write; no other envs |
| `adjuster`  | all envs read; adjudication transitions in UAT + Prod |
| `senior_adjuster` | all adjuster rights + high-value approval + reserve override |
| `finance_operator` | payout instruction + read access |
| `operations` | reports + read access; no mutations |
| `release_operator` | prod promotion approval |
| `genai_service_owner` | manage the genai-gateway; cannot promote |
| `auditor` | audit log read-only; no app data access |
| `sre_oncall` | runbooks + monitoring; no app data access |
| `devops` | CI/CD management; no app data access |

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Realm export | Open | `realm-export.json` |
| Realm bootstrap | Open | Ansible role in `infra/ansible/roles/auth_realm/` |
| Client per service | Open | One confidential client per app + per agent |

## References

- `security-baseline.md`
- `ADR-006` (human approval roles)
- `docs/architecture/promotion-pipeline.md`
