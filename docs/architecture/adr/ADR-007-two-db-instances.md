# ADR-007: Two database instances only — QA and Production

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-007 (`01_ARS.docx` REQ-13: "Central database concept, refined to two DB instances: QA and Production")
- **Supersedes:** —

## Context

The stakeholder refined the "central database" concept to exactly two
instances: one for QA, one for Production. Dev and UAT share the QA instance
or use disposable schemas; no additional instances were requested.

## Decision

- Exactly two PostgreSQL instances exist: QA and Production.
- Dev runs against the QA instance (or a local container for developers).
- UAT uses the QA instance with UAT-specific schemas/roles, unless the
  stakeholder later funds a third instance.

## Consequences

- Test data isolation between UAT and QA test runs must be handled with
  schemas and roles, not separate instances.
- The DB sync pipeline (ADR-008) only ever concerns QA and Production.
