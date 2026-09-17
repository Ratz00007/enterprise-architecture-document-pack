# Central GenAI Gateway

The *only* service that may talk to an LLM provider. Every other
component goes through this gateway, which means:

- **Provider swap is a config change.** Today OpenAI, tomorrow
  Anthropic, next month an on-prem model — the application code
  doesn't change. (`ADR-005`.)
- **The data-class policy is enforced here, not in the
  application.** A request that contains `production_pii` is
  rejected before it leaves the gateway. (`ADR-011`.)
- **The GenAI host has no other credentials.** It cannot deploy,
  cannot write to any database except its own audit log, cannot
  promote anything. (Enforced by absence, not by policy text.)
- **The audit log records every prompt, every response, every
  provider metadata.** `services/audit-log/`.

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| LiteLLM base image | Drafted | See `config/litellm-config.yaml` |
| Provider config | Open | Locked when stakeholder picks a provider |
| Data-class policy | Drafted | See `policies/data-class.yaml` |
| Per-env allow-list | Open | Driven by the per-env inventory |
| Audit log integration | Open | Wired to `services/audit-log/` post-MVP |
| RAG context retrieval | Open | pgvector on the same Postgres instance |

## Run locally

```bash
docker run -d \
  --name litellm \
  -p 4000:4000 \
  -v $PWD/config:/app/config \
  -e LITELLM_CONFIG=/app/config/litellm-config.yaml \
  ghcr.io/berriai/litellm:main-latest
```

## Key files

- `config/litellm-config.yaml` — model list, routing, retry policy
- `policies/data-class.yaml` — per-environment data class allow-list
- `policies/action-policy.yaml` — what actions the gateway will vs. will not mediate

## References

- `ADR-004` Central GenAI
- `ADR-005` Vendor-agnostic GenAI
- `ADR-011` Data boundary and gateway
- `docs/architecture/observability-design.md` (alerting on the gateway)
