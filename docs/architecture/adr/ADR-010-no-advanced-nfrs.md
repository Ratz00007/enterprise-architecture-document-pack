# ADR-010: No invented non-functional requirements

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-010**, with the
> guardrail restated as a project-side discipline.

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-010
- **Supersedes:** —

## Context

The pack explicitly says: *"No specific scalability, availability,
performance or advanced security targets were supplied; do not invent
them."* (`01_ARS.docx` §2). This is a guardrail, not a constraint
that prevents us from doing good work — it is what stops us from
making promises the stakeholder did not ask for.

## Decision

- We do not invent performance targets (RPS, p99, throughput).
- We do not invent availability targets (uptime, RPO, RTO).
- We do not invent advanced security controls (HSM, DLP, CASB)
  beyond the confirmed baseline.
- We do not invent "enterprise" features (multi-region, active-
  active, geo-DNS).
- We **do** ship with thresholds that are *user-noticeable* defaults
  (see `observability-design.md`) and document them as defaults,
  not as targets.
- When a real requirement shows up (the stakeholder supplies a
  number), it goes through a new ADR before it changes the build.

## Consequences

**Positive**

- The system is built to the *stated* requirements, not to
  someone's gut feeling about what "enterprise" means.
- No false promises. If we don't have an RPO number, we say so.
- Reviewers can verify every threshold against the pack.

**Negative**

- Some readers will be uncomfortable with the lack of NFR
  numbers. That discomfort is the *point* — the pack is explicit
  that those numbers are not yet supplied.

**Neutral**

- The 24-hour log retention (`ADR-009`) is a *confirmed* target
  and is therefore allowed to be in scope. The distinction is
  "confirmed by the stakeholder" vs. "would be nice".

## Operationalisation

- Every ADR must answer the question: *is this requirement
  confirmed in the pack, or invented here?* If invented, the ADR
  is rejected at review.
- Every "alert threshold" in `infra/monitoring/prometheus/rules/`
  is annotated with the source: `default` (we chose it),
  `confirmed` (stakeholder said so), or `derived` (calculated
  from a confirmed number).
- Every "feature flag" or "future hardening" note in the docs
  is marked with the ADR that would unlock it.

## References

- `01_ARS.docx` §2, §4
- `10_RTM_ADR.docx` §2 ADR-010
- Every other ADR — this one is the meta-rule that bounds them all.
