# Platform Services

Each service in this directory is a deployable component that
supports the product (`apps/`) but is **not** part of the product
itself. They are versioned, tested, and released through the same
pipeline as the apps.

| Service | What it does | Owner | ADR |
|---------|--------------|-------|-----|
| [`genai-gateway/`](./genai-gateway/) | LiteLLM-based vendor-neutral GenAI gateway. The *only* host that may talk to a model provider, the *only* host with outbound internet for that purpose. | platform-genai | `ADR-004`, `ADR-005`, `ADR-011` |
| [`db-sync/`](./db-sync/) | Pipeline-mediated sanitized image of prod → qa/uat DBs. | data | `ADR-007`, `ADR-008` |
| [`audit-log/`](./audit-log/) | Append-only, hash-chained audit trail. Writers: every state-mutating action in the API; every promotion in Jenkins; every GenAI call. | platform-sec | `ADR-006`, `ADR-011` |
| [`auth-realm/`](./auth-realm/) | Keycloak realm export (OIDC). Source of truth for roles, clients, scopes, and authentication policies. | platform-sec | (post-MVP depth) |
| [`log-aggregator/`](./log-aggregator/) | Promtail / journald / rsyslog configuration, shipped to every host by the `common` role. | platform-sre | `ADR-009` |

## Service directory template

Every service follows the same shape:

```
services/<name>/
├── README.md          # what it is, who owns it, how to run it
├── Containerfile      # distroless where possible
├── config/            # default config; environment overrides at runtime
├── src/               # code
└── tests/             # unit + integration tests
```

## Operating principle

A service exists only if removing it would break a *confirmed*
requirement in the pack. If a service is speculative, it is not
here — it is in the deferred list in
[`docs/architecture/adr/INDEX.md`](../docs/architecture/adr/INDEX.md).
