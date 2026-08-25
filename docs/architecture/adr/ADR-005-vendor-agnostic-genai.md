# ADR-005: Vendor-agnostic GenAI

> **Transcription of pack `10_RTM_ADR.docx` §2 ADR-005**, with the
> implementation contract spelled out under "Operationalisation".

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** Pack ADR-005
- **Supersedes:** —

## Context

The pack requires the GenAI capability to be vendor-neutral. The
stakeholder's example list included OpenAI and Anthropic; both are
examples, not mandatory choices. The point is to avoid lock-in to a
single model provider, and to make provider swap a configuration
change, not a code change.

## Decision

- All application and pipeline code calls the GenAI gateway, never
  a model provider directly.
- The gateway is **LiteLLM** (`services/genai-gateway/`). It
  provides a single OpenAI-compatible API and routes to any
  configured provider backend.
- Provider choice is a config file (`services/genai-gateway/config.yaml`)
  and an environment variable. No code change is required to swap
  providers.
- The list of supported providers, their quotas, their per-env
  allow-lists, and their per-action policies all live in config,
  not in code.

## Consequences

**Positive**

- Provider swap is a config change. If the current provider's
  pricing changes, or their terms change, we can move to another
  provider without a code release.
- The "no code knows about a specific provider" property is
  testable by an ESLint / Checkstyle rule.

**Negative**

- LiteLLM is one more thing to operate. The trade-off is acceptable
  given the lock-in cost of skipping it.

**Neutral**

- The contract: the gateway is the only allowed caller of any
  provider. Any direct call from application code to a provider
  endpoint is a CI build break (enforced by egress firewall
  allow-list and by code review).

## Operationalisation

- LiteLLM config in `services/genai-gateway/config.yaml`.
- Provider credentials live in Ansible Vault, mounted at runtime.
- Egress firewall on `genai-01` allow-lists only the chosen
  provider endpoints.
- The application uses the `openai` SDK against the gateway URL —
  the swap to a non-OpenAI provider is invisible to the
  application.

## References

- `01_ARS.docx` §2, §9, §10
- `05_GenAI_Architecture.docx` §3
- `10_RTM_ADR.docx` §2 ADR-005
- `ADR-004` (central GenAI)
- `ADR-011` (data boundary)
- `services/genai-gateway/`
