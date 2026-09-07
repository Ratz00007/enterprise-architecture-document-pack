# services/genai-gateway — Central, vendor-neutral GenAI (ADR-004/005/011)

## Components

- **`litellm.config.yaml`** — the LiteLLM proxy configuration. Vendor
  neutrality lives here: routing to a different provider is a config change
  plus an environment variable, never a code change. In dev, the
  `acme-advisory` model routes to the local sandbox provider, so the stack
  runs with no external vendor and no data leaving the machine.
- **`sandbox-provider/`** — OpenAI-compatible stub used by the dev gateway.
  It enforces the data-classification policy before anything reaches a
  model: direct identifiers (SSN, card numbers, emails, phone numbers per
  `pii-rules.json`) are rejected with HTTP 422. Every response is explicitly
  advisory.
- **`sandbox-provider/policy.json`** — the per-environment allow-list
  (ADR-011): dev allows advisory interactions on `INTERNAL`-classified data,
  QA only masked data, UAT/prod deny model access entirely. Production
  receives no direct inbound GenAI traffic.

## Local run

```bash
docker compose up sandbox-provider genai-gateway
# gateway on :9000 (OpenAI-compatible); sandbox provider on :9100
```

## Connecting the Claims API

`GENAI_GATEWAY_URL` points at the gateway (`http://genai-gateway:4000` in
compose). The API client (`com.acme.claims.ai.GenAIGatewayClient`) only uses
the OpenAI-compatible chat completion route; domain prompts (e.g. triage
assessment) are composed by callers. **Gateway output is advisory only —
humans decide** (ADR-006).

## Production

Set the upstream provider credentials from the secret store (Ansible Vault):
`GENAI_UPSTREAM_BASE_URL`, `GENAI_UPSTREAM_API_KEY`, `GENAI_MASTER_KEY`.
Never commit them; the checked-in defaults are dev-only throwaways.
