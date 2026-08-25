# ADR-002: Four sequential environments

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-002**, with project-side
> implementation notes added under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-002
- **Supersedes:** —

## Context

The pack confirms a strict sequential promotion order
`Dev → QA → UAT → Production`. No environment may be skipped. No
parallel fan-out. No Dev → Prod shortcut. This is a confirmed
constraint, not a recommendation.

## Decision

We run exactly four environments, named exactly `dev`, `qa`, `uat`,
`prod`. The promotion order is fixed and cannot be reconfigured
without a new ADR and stakeholder sign-off.

| Environment | Purpose | Owns its own data? | UAT users? |
|-------------|---------|--------------------|------------|
| dev | Developer builds, unit tests, local stack | Yes (ephemeral) | No |
| qa | Automated + manual QA, contract tests | Yes (sanitized prod image) | No |
| uat | Business validation by named users | Yes (sanitized prod image) | Yes |
| prod | Live traffic | Yes (source of truth) | No |

## Consequences

**Positive**

- The pipeline can be modelled as a finite state machine. Every
  transition has a defined evidence requirement and a defined
  approver role.
- Audit trail is straightforward: every release event is one row.

**Negative**

- Slow. UAT feedback loops can be hours. This is the explicit
  trade-off the stakeholder chose.
- No canary / blue-green. Stakeholder has been told; deferred to
  hardening phase per `ADR-010`.

**Neutral**

- "No environment may be skipped" is enforced by the Jenkins
  pipeline, not by policy. There is no manual override path in the
  current implementation.

## Operationalisation

- The Jenkins Shared Library encodes the four-stage pipeline as
  four reusable steps: `promoteToQA`, `promoteToUAT`,
  `promoteToProd`, plus the inverse `rollbackFrom<Env>`.
- Each stage's approval is a Keycloak-gated API call. The Jenkins
  job cannot bypass the approval step; removing the approval step
  from the Jenkinsfile is a code change that requires PR review.
- The audit log is written by the pipeline, not by the approver's
  client, so even a compromised approver credential can't forge
  history.

## References

- `01_ARS.docx` §2, §6
- `02_HLA.docx` §3, §4
- `04_CICD_DevOps.docx` §2, §4
- `10_RTM_ADR.docx` §2 ADR-002
- `promotion-pipeline.md`
