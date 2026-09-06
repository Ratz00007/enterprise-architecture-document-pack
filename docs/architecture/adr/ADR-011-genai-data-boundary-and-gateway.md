# ADR-011: GenAI gateway via LiteLLM; production data is allow-listed, never default

- **Status:** Accepted
- **Date:** 2026-08-25
- **Origin:** This project (implementation contract for pack ADR-004/ADR-005)
- **Supersedes:** —

## Context

The pack requires a central, vendor-agnostic GenAI capability integrated with
the SDLC flow. It leaves hosting location, gateway and data-handling controls
as open decisions (`01_ARS.docx` §5). `06_Data_Architecture.docx` and
`09_Security_Network.docx` flag production data exposure as a design risk.

## Decision

- The only GenAI entry point is a central **LiteLLM** gateway
  (`services/genai-gateway/`); applications never call model providers.
- The gateway exposes an OpenAI-compatible API, so provider swap is
  config-only.
- Data classification governs what may be sent: production data is
  **allow-listed** per interaction type, never default. By default only
  metadata/telemetry flows outward; production receives no direct inbound
  GenAI connectivity.
- Gateway output is **advisory only**: humans decide (ADR-006).

## Consequences

- `apps/claims-api` talks to the gateway through a single client
  (`com.acme.claims.ai.GenAIGatewayClient`); assessments feed human
  triage/adjudication decisions and are never executed automatically.
- Per-environment allow-lists live in gateway config, not in application code.
