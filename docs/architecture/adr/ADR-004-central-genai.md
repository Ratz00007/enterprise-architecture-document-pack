# ADR-004: Central GenAI

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-004**, with
> implementation notes captured under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-004
- **Supersedes:** —

## Context

The pack requires a single central GenAI capability that acts as the
intelligence layer across all four environments. Its purpose is to
"synchronize approved information flows and support package lifecycle
management, test analysis, and complex issue diagnosis" (`05_GenAI_Architecture.docx` §1).

The capability map (`05_GenAI_Architecture.docx` §2) lists seven
required behaviors, all advisory except the "Human approval" gate,
which is mandatory and not negotiable.

## Decision

- We operate exactly one GenAI service: `genai-01`, running the
  LiteLLM gateway (see `ADR-005`, `ADR-011`).
- All four environments may consume the GenAI service through
  allow-listed endpoints only. No environment may host its own
  GenAI capability.
- The GenAI service has **no** credentials that would let it write
  to any environment's database, deploy any artifact, or modify the
  audit log. All its actions are advisory and require a human to
  act on them.

## Consequences

**Positive**

- One place to govern prompts, policies, and audit.
- The "no autonomous authority" property is testable: it is a
  property of the GenAI host's credential set.

**Negative**

- Single point of failure for intelligence. Mitigated by the
  "operable without GenAI" principle (`11_Operations_Runbooks.docx` §4).
- All environments' "smart" behavior is correlated with one
  service's availability.

**Neutral**

- The "operable without GenAI" requirement means every workflow
  must have a human-only fallback. This is an explicit design
  constraint, not a stretch goal.

## Operationalisation

- `genai-01` is provisioned by the `infra/ansible/roles/genai_gateway/`
  role and exposes a single HTTPS port (8443) with mutual TLS.
- The service is registered in Keycloak as a confidential client.
  Every call carries a JWT scoped to the calling environment.
- The audit log records every prompt, every response, and every
  action the GenAI attempted.
- See `ADR-011` for the data-boundary rules and `ADR-005` for the
  vendor-neutrality contract.

## References

- `01_ARS.docx` §2, §9
- `02_HLA.docx` §3
- `05_GenAI_Architecture.docx` (entire document)
- `10_RTM_ADR.docx` §2 ADR-004
- `ADR-005` (vendor neutrality)
- `ADR-011` (data boundary)
