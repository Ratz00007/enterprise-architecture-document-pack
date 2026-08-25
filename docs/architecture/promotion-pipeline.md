# Promotion Pipeline

> The state machine that moves a build from a developer's laptop to
> production. Every gate, every approver, every artifact identity
> recorded for audit.

## Stages

```
       ┌────────┐    ┌────────┐    ┌────────┐    ┌──────────┐
       │  Dev   │───▶│  QA    │───▶│  UAT   │───▶│   Prod   │
       └────────┘    └────────┘    └────────┘    └──────────┘
            │             │             │              │
        developer      QA lead      UAT lead      DevOps operator
          (self)       approves      approves       approves
```

No stage may be skipped. The pipeline enforces ordering. Even the
release operator cannot move a build from QA to Prod without a UAT
record.

## Per-stage gate

| Stage | Trigger | Required evidence | Human approver | What can fail the gate |
|-------|---------|-------------------|----------------|------------------------|
| **Dev** | Push to `main` or PR merge | Build OK, unit tests OK, SAST clean, coverage ≥ 80% line / 70% branch, Docker image built & signed | Developer (self-approve of own PR) | Compilation, unit test, static analysis, dependency CVE |
| **QA** | Tag `qa-<semver>` or promotion button | Container image promoted; Testcontainers integration OK; contract tests OK; manual QA pass recorded; no `WARN` Prometheus alerts fired on the smoke run | QA lead | Integration test, contract test, manual QA evidence, CVE |
| **UAT** | Tag `uat-<semver>` or promotion button | UAT users signed off via the UAT portal; at least one business scenario per workflow passed; no `ERROR` in the last 24h smoke | UAT lead (or business owner) | UAT portal evidence, SLO breach, manual rejection |
| **Prod** | Tag `prod-<semver>` or scheduled release window | Release runbook followed; rollback rehearsal in last 30 days; oncall acknowledged; GenAI pre-release summary attached (advisory) | DevOps release operator | Rollback rehearsal missing, oncall ack missing, open Sev-1 incident, GenAI hard-blocks (none defined yet — advisory only) |

## Artifact identity

Every promotion moves an **immutable artifact** by digest, never by
tag. Tag is for humans, digest is for machines.

```
claims-api:1.4.2          # human tag
sha256:9f1c…d7e2          # canonical digest
```

The image is built once in the Dev stage and signed with
[cosign](https://docs.sigstore.dev/cosign/overview/) keyless against
the internal OIDC issuer. Every subsequent stage re-verifies the
signature before promoting.

## Approval workflow

- **UI:** the Jenkins Blue Ocean "Approve" step, or the dedicated
  release console at `https://release.acme.internal/`.
- **API:** `POST /api/v1/releases/{id}/approve` with a signed JWT
  tied to the approver's Keycloak role.
- **Audit:** every approval writes an entry to
  `services/audit-log` with:
  - actor (Keycloak subject)
  - role asserted (e.g. `qa_lead`)
  - artifact digest
  - stage
  - timestamp (UTC)
  - human-readable comment
  - optional ticket reference

## Promotion commands (CLI)

```powershell
# Promote the latest Dev build to QA
pwsh -File ci\scripts\promote.ps1 -Env qa -Build 142

# Approve the QA candidate (must be a QA lead in Keycloak)
pwsh -File ci\scripts\approve.ps1 -Env qa -Build 142 -Comment "All green"

# Promote to UAT after QA approval
pwsh -File ci\scripts\promote.ps1 -Env uat -Build 142

# Roll back from Prod to the previous digest
pwsh -File ci\scripts\rollback.ps1 -Env prod -ToDigest sha256:9f1c…d7e2
```

## Rollback

- Every release records the previous digest + the change that
  triggered the rollback in the audit log.
- A rollback is itself a promotion: it goes through the same gates
  (DevOps operator approval) and is logged identically.
- Post-rollback: open a Sev-2 incident, run the rollback runbook
  (`docs/runbooks/prod-rollback.md`), notify UAT.

## GenAI in the pipeline

GenAI is **advisory only**. It may:

- Summarise the diff between two candidate builds.
- Cluster test failures and propose likely root causes.
- Pre-fill the release evidence bundle.
- Recommend rollback vs forward-fix when an incident happens.

GenAI **may not**:

- Approve a promotion (no API surface, by design).
- Push to a registry.
- Modify the audit log.
- Communicate with production without going through the GenAI
  gateway, which itself is rate-limited, allow-listed, and audited.

This is enforced architecturally: GenAI runs on `genai-01`, has no
write credentials anywhere else, and all its outbound calls pass
through a per-action policy layer in `services/genai-gateway/`.

## Why sequential, not blue/green or canary

The pack says "sequential Dev → QA → UAT → Production" and "no
bypass path". Blue/green, canary, and feature flags are not the
baseline. They are explicitly flagged for the hardening phase.

The justification: when a Fortune 500 stakeholder asked for "basic
sequential promotion with human gates", that is what we build first.
Bypass paths come later, with their own ADR.
