# ADR-012: Insurance claims domain scope

- **Status:** Accepted
- **Date:** 2026-08-25
- **Deciders:** Ratin Sharma (stakeholder), Mavis (architect lead)
- **Supersedes:** —
- **Superseded by:** —

## Context

The pack describes the hosting environment for "one application"
without specifying the application. The stakeholder has chosen
**insurance claims processing** as the representative Fortune 500
domain we will build, because:

- It exercises every part of the architecture: stateful business
  workflow, audit trail, role-based access, document handling,
  human approval at multiple gates, regulatory reporting, sensitive
  PII handling.
- It is a real, well-understood problem in the Fortune 500
  insurance carrier space, with realistic state machines and
  regulatory pressure.
- It is a domain where the "human approves, AI advises" pattern is
  not optional.

## Decision

The product (`apps/claims-api/`, `apps/claims-web/`) implements
**insurance claims processing** end to end, from First Notice of
Loss (FNOL) through final payout, with the following workflows in
scope for MVP:

### In scope (MVP)

1. **FNOL intake** — claim creation from a customer or agent,
   including policy verification and initial triage.
2. **Document intake** — upload, classify, and link supporting
   documents to a claim.
3. **Adjudication** — adjuster-driven decision workflow, with
   fraud-signal hooks and SLA tracking.
4. **Reserve management** — set, adjust, and release financial
   reserves against a claim.
5. **Payout** — generate and route the payout instruction, with
   audit-grade provenance.
6. **Audit and reporting** — every state change is logged; standard
   reports (open claims, cycle time, loss ratio) are available to
   the operations role.

### In scope (post-MVP, tracked separately)

- Subrogation and recovery workflows.
- Litigation tracking.
- Reinsurance bordereaux.
- Customer self-service portal beyond FNOL.
- Mobile adjuster app.

### Out of scope (must be re-specified if added)

- Underwriting / policy issuance.
- Billing / premium collection.
- Agent / broker commission management.
- Any product line beyond first-party property & casualty.

## Consequences

**Positive**

- Realistic complexity. The state machine and the audit trail
  are non-trivial, which is what makes this a credible proof of
  capability.
- Maps cleanly to the pack's requirements: human approval at
  every state transition, GenAI assist at every workflow step,
  PII handling discipline, regulator-friendly audit trail.

**Negative**

- The domain is large. We will *not* ship all of it in one go;
  MVP is a working FNOL-to-payout slice with one product line.
- Insurance has its own jargon; the codebase and the docs use
  it deliberately so the vocabulary is correct on review.

**Neutral**

- The data model captures the minimum set of entities required
  for the MVP. Anything else waits for a follow-up ADR.

## Operationalisation

- Domain entities: `docs/domain/entities.md`.
- Workflows: `docs/domain/workflows.md`.
- Vocabulary: `docs/domain/glossary.md` (TBD).
- Compliance mapping: `docs/compliance/` (SOC2, GDPR, NAIC).

## References

- `ADR-000` (tech stack)
- `ADR-004`, `ADR-005`, `ADR-011` (GenAI integration)
- `ADR-006` (human approval)
- `apps/claims-api/`
- `apps/claims-web/`
- `docs/domain/`
