# ADR-006: Human approval mandatory

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-006**, with the
> enforcement mechanism captured under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-006
- **Supersedes:** —

## Context

The pack explicitly forbids autonomous production deployment. Every
promotion to the next environment requires a human approver from the
role designated for that gate. GenAI is explicitly excluded from
holding promotion authority.

## Decision

- The Jenkins pipeline **always** stops at each environment's
  approval gate. There is no `--force` flag, no "skip approval"
  option, no "auto-approve if GenAI says so" path.
- The approver's Keycloak role is asserted by the API, not claimed
  by the client. The CI cannot forge a role.
- The audit log entry is written **after** the role assertion
  succeeds and **before** the promotion is queued. There is no path
  where a promotion happens without an audit row.
- The "GenAI cannot approve" property is enforced by *absence of
  credentials* on the GenAI host, not by policy text. The GenAI
  host has no deploy or promotion credentials; therefore it
  physically cannot approve anything.

## Consequences

**Positive**

- The "no autonomous prod deploy" property is provable, not just
  promised.
- Audit trail is complete: every promotion has a human actor with
  a real Keycloak subject.

**Negative**

- A malicious or careless approver can still approve a bad build.
  Mitigations:
  - The pipeline records the approver, the artifact digest, and
    the prior environment's evidence bundle.
  - Rollback is a first-class promotion (see
    `promotion-pipeline.md`), so a bad approval is recoverable.
  - Post-MVP: the `genai_service_owner` role is empowered to
    require a second approver after any Sev-1 incident.

**Neutral**

- Override process is intentionally not defined yet. The pack
  doesn't supply it; the hardening phase can add one with its own
  ADR.

## Operationalisation

- `ci/scripts/approve.ps1` is the human-facing entry point. It
  requires a valid Keycloak token with the right role.
- The Jenkinsfile uses `input` step with `submitter` parameter set
  to the named Keycloak group. Anyone outside the group sees a
  read-only pipeline.
- Every approval writes to `services/audit-log/` with:
  - `actor_sub` (Keycloak subject)
  - `actor_role` (e.g. `qa_lead`)
  - `artifact_digest`
  - `from_env`, `to_env`
  - `comment`
  - `ticket_ref` (optional)
  - `timestamp_utc`

## References

- `01_ARS.docx` §11
- `04_CICD_DevOps.docx` §4, §5
- `05_GenAI_Architecture.docx` §2, §5
- `10_RTM_ADR.docx` §2 ADR-006
- `promotion-pipeline.md`
- `services/audit-log/`
