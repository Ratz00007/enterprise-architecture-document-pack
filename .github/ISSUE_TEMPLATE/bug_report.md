---
name: Bug report
about: Report a defect in the Acme Claims platform
title: "[BUG] "
labels: ["bug", "needs-triage"]
assignees: []
---

## What happened

A clear, one-sentence description of the bug.

## Steps to reproduce

1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected behaviour

What should have happened instead.

## Environment

- Component: `claims-api` / `claims-web` / `genai-gateway` / `db-sync` / `auth-realm` / `log-aggregator` / `infra`
- Environment: `dev` / `qa` / `uat` / `prod`
- Build / version: `<git rev-parse HEAD>`, `<build number>`
- Browser (if web): `<browser + version>`

## Logs and traces

Attach or link the relevant log query, Prometheus alert, or
correlation ID. PII must be redacted.

## Severity

- [ ] Sev-1 (customer impact, page now)
- [ ] Sev-2 (degraded service, on-call ack within 1h)
- [ ] Sev-3 (latent, on-call ack within 4h)
- [ ] Sev-4 (hygiene, backlog)

## Linked items

- ADR: <link or "none">
- Runbook: <link or "none">
- Related PR / issue: <link or "none">
