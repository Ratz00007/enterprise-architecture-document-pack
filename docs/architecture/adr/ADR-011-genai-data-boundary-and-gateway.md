# ADR-011: GenAI data boundary and gateway

- **Status:** Accepted
- **Date:** 2026-08-25
- **Deciders:** Mavis (architect lead)
- **Supersedes:** —
- **Superseded by:** —

## Context

`ADR-004` and `ADR-005` establish that we have one central,
vendor-neutral GenAI capability. They do not yet say **what data
the GenAI is allowed to see**, **what actions it is allowed to
take**, or **how that boundary is enforced**.

The pack is explicit (`05_GenAI_Architecture.docx` §4) that
"the central GenAI system should not automatically receive
unrestricted production data. The implementation must define
approved data classes, masking/sanitization, access policy,
retention and auditability before production connectivity is
enabled."

## Decision

### Gateway

- All GenAI calls from any environment go through
  `services/genai-gateway/` (LiteLLM), never directly to a
  provider.
- The gateway is the *only* host with provider credentials and
  the *only* host with outbound internet access to the provider.
- The gateway has **no** credentials for any database, deploy
  target, audit-log writer, or promotion API. Its actions are
  advisory only.

### Data classes

| Class | Definition | Default GenAI access |
|-------|------------|----------------------|
| `public` | Documentation, ADRs, public docs | allowed |
| `internal` | Architecture, runbooks, dashboards metadata | allowed, no PII |
| `operational` | App logs (post-PII-strip), metrics, traces | allowed, sampled |
| `synthetic_test` | Test data, fixtures, mocks | allowed |
| `production_pii` | Real PII from prod | **denied by default**; allow-list per environment per action |
| `production_secrets` | Credentials, tokens, keys | **denied always** |

### Per-environment access

| Environment | Default data classes allowed |
|-------------|------------------------------|
| dev | `public`, `internal`, `synthetic_test` |
| qa | `public`, `internal`, `operational`, `synthetic_test` |
| uat | `public`, `internal`, `operational` (no `production_pii`) |
| prod | `public`, `internal`, `operational` (sampled); `production_pii` only via explicit, time-bounded approval |

### Actions

- The gateway can call the LLM provider.
- The gateway can call back into the **read-only** API of each
  environment for context retrieval (RAG).
- The gateway **cannot** call any write API. There is no path
  from GenAI to a database write, a deploy action, or an audit
  log entry that originates with GenAI. (The audit log *records*
  GenAI activity; the gateway has a write role to the audit log
  table for that purpose, but it is denied to anything else.)

### Auditability

- Every prompt and every response is written to the audit log
  with the caller, the environment, the data classes touched,
  the provider, the model, the token count, and the cost (if
  metered).
- Retention: 7 years for the audit log (compliance-aligned).
- Sampling for the `operational` class is configurable per
  environment; default is 10% sample for prod, 100% for
  dev/qa/uat.

## Consequences

**Positive**

- The "no production data leaks to GenAI by default" property is
  *configuration*, not policy text. The CI verifies the
  configuration on every build.
- Provider swap (`ADR-005`) is unchanged — the gateway still
  abstracts the provider.
- The audit trail is rich enough to answer "did GenAI ever see
  field X about subject Y?" — important for compliance.

**Negative**

- The matrix above will need adjustment as real workloads arrive.
  It is a starting point, not a permanent classification.
- `production_pii` access is theoretically possible; the discipline
  is that it requires an explicit, time-bounded approval and is
  logged. This is friction on purpose.

**Neutral**

- RAG is built on the same Postgres 16 instances (via pgvector).
  This means the RAG store inherits the same backup / restore /
  audit story as the operational data.

## Operationalisation

- Gateway code: `services/genai-gateway/`.
- Config: `services/genai-gateway/config.yaml` declares
  providers, models, and the data-class matrix.
- Per-environment allow-list: `services/genai-gateway/policies/`.
- Egress firewall: `genai-01` is the only host with outbound
  internet. Allow-list includes only the chosen LLM provider
  endpoints.
- CI check: the build fails if any application or pipeline module
  imports a provider SDK directly. Only `services/genai-gateway/`
  may import a provider SDK.

## References

- `01_ARS.docx` §9, §10
- `05_GenAI_Architecture.docx` §3, §4, §5
- `10_RTM_ADR.docx` §2 ADR-004, ADR-005
- `ADR-004` (central GenAI)
- `ADR-005` (vendor neutrality)
- `security-baseline.md`
- `services/genai-gateway/`
