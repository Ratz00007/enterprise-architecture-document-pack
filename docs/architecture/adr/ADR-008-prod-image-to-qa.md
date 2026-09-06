# ADR-008: Production image replicated to QA via controlled pipeline

- **Status:** Accepted (mechanism TBD)
- **Date:** 2026-08-25
- **Origin:** Pack ADR-008 (`01_ARS.docx` REQ-14, `06_Data_Architecture.docx` §2–§4)
- **Supersedes:** —

## Context

The pack requires synchronization via pipeline and replication of a Production
database image to the QA image. The exact direction, technology and
sanitization mechanism remain implementation decisions
(`06_Data_Architecture.docx` flags the masking question as critical).

## Decision

- Refresh flow: approved Production snapshot → controlled QA derivative →
  masking/sanitization (ADR-017) → restore into QA → integrity checks →
  provenance recorded.
- The replication always flows Production → QA, never the reverse.
- The mechanism (snapshot technology, transport, schedule) stays TBD and is
  owned by the data workstream.

## Consequences

- QA can never be a source of truth for production data.
- Every refresh must run the masking pipeline before QA availability; an
  unmasked restore is a security incident, not a shortcut.
