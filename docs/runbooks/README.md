# Runbooks

Operational runbooks for the failure modes the pack calls out
(`11_Operations_Runbooks.docx` §2) and the failure modes we add
along the way.

| Runbook | When to use | Owner |
|---------|-------------|-------|
| `prod-rollback.md` | A bad release is live in prod | devops oncall |
| `db-down.md` | Postgres unreachable | sre oncall |
| `db-sync-failed.md` | The prod → qa sync job failed | data oncall |
| `genai-degraded.md` | The GenAI gateway is 5xx or slow | platform-genai oncall |
| `promotion-stalled.md` | A candidate is waiting for approval > 4h | devops oncall |
| `log-rotation-stuck.md` | The 24h rotation hasn't completed | sre oncall |
| `pci-exposure.md` | PII detected in an unexpected place | security oncall |
| `keycloak-down.md` | Auth gateway unreachable | sre oncall |

## How to use a runbook

1. Confirm the alert matches the trigger described in the runbook.
2. Walk the runbook top to bottom. Do not skip steps.
3. If a step fails, stop and call the secondary oncall.
4. When the runbook says "open an incident", do it. Don't try to
   fix a Sev-1 in the runbook.

## How to write a runbook

1. Trigger: which alert or symptom
2. Triage: how to confirm this runbook applies
3. Mitigation: the steps to stop the bleeding
4. Recovery: the steps to restore normal service
5. Postmortem hooks: what to capture for the postmortem

A runbook is a contract. Missing a runbook means the alert is not
allowed to be Sev-1 (per `observability-design.md`).
